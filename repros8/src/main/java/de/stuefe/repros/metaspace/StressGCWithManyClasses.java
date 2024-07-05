package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.TestCaseBase;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;

@CommandLine.Command(name = "ManyClassesManyObjectsFullGC", mixinStandardHelpOptions = true,
        description = "ManyClassesManyObjectsFullGC repro.")
public class StressGCWithManyClasses extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--autoyes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    public static void main(String... args) {
        int exitCode = new CommandLine(new StressGCWithManyClasses()).execute(args);
        System.exit(exitCode);
    }


    @CommandLine.Option(names = { "--num-classes", "-C" }, description = "Number of classes (default: ${DEFAULT_VALUE})")
    int numClasses=256;

    @CommandLine.Option(names = { "--objects", "-n" }, description = "Number of objects per class (default: ${DEFAULT_VALUE})")
    int numObjectsPerClass=50000;

    @CommandLine.Option(names = { "--cycles", "-c" }, description = "Number of GC cycles (default: ${DEFAULT_VALUE})")
    int cycles = 10;

    @CommandLine.Option(names = { "--print-hashes" }, description = "Print hashes (default: ${DEFAULT_VALUE})")
    boolean printHashes = false;

    @CommandLine.Option(names = { "--randomize" }, description = "Randomizes object classes (off: strict interleaving) (default: ${DEFAULT_VALUE})")
    boolean randomize = true;

    @CommandLine.Option(names = { "--class-stride" }, description = "If not --randomize: stride by which we alter the class per object (default: ${DEFAULT_VALUE})")
    int classStride = 1;

    @CommandLine.Option(names = { "--ball-of-yarn" }, description = "If true, all live objects are wired up as a (randomized, if --randomize) object graph. If false, they are all in a flat array. (default: ${DEFAULT_VALUE})")
    boolean ballOfYarn = true;

    static Object[] RETAIN; // array mode
    static Object BALL_OF_YARN; // ball-of-yarn mode

    static String generateSource(int numOops) {
        StringBuilder bld = new StringBuilder();
        bld.append("public class CLASSNAME {");
        for (int i = 0; i < numOops; i++) {
            bld.append("public Object o");
            bld.append(i);
            bld.append(';');
        }
        bld.append("};");
        return bld.toString();
    }

    static boolean isDirectlyConnected(Object referer, Object referee, Field[] o1Fields) throws IllegalAccessException {
        for (Field f : o1Fields) {
            if (f.get(referer) == referee) {
                return true;
            }
        }
        return false;
    }

    final static void swapSlots(int[] arr, int x, int y) {
        int tmp = arr[x];
        arr[x] = arr[y];
        arr[y] = tmp;
    }

    Object buildBallOfYarn(Object[] all, Random r) throws IllegalAccessException {
        // for speed, build up array of fields. Each object has at least one oop field (o0), at most four
        Field[][] fieldArrays = new Field[all.length][];
        for (int i = 0; i < all.length; i++) {
            fieldArrays[i] = all[i].getClass().getFields();
        }

        int[] referredToBy = new int[all.length];
        int[] refers = new int[all.length];
        for (int i = 0; i < all.length; i ++) {
            referredToBy[i] = i - 1;
            refers[i] = i + 1;
        }
        referredToBy[0] = all.length - 1;
        refers[all.length - 1] = 0;

        // shuffle
        if (randomize) {
            for (int i = 1; i < all.length; i ++) {
                int swapWith = Math.max(1, r.nextInt(all.length));
                swapSlots(refers, i, swapWith);
                swapSlots(referredToBy, i, swapWith);
            }
        }
        // now wire objects up.
        boolean[] empties = new boolean[all.length];
        for (int i = 0; i < all.length; i++) {
            Field[] fields = fieldArrays[i];
            // wire up first field according to the precalculated shuffled refers array
            fields[0].set(all[i], all[refers[i]]);
            // Wire up o1..o3 in whatever direction
            for (int j = 1; j < fields.length; j++) {
                int targetIdx = randomize ? r.nextInt(fields.length) : (i % fields.length);
                fields[j].set(all[i], all[targetIdx]);
            }
        }

        return all[0];
    }

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        System.out.println("Num Classes: " + numClasses);
        System.out.println("Num Objects per Class: " + numObjectsPerClass);
        System.out.println("Cycles: " + cycles);
        System.out.println("randomize? " + randomize);
        System.out.println("Mode: " + (ballOfYarn ? "ball-of-yarn" : "array"));

        Random r = new Random(0x12345678);

        int numMinOopFields = 1;
        int numMaxOopFields = 4;

        System.out.print("Generate " + numClasses + " classes...");
        for (int i = 0; i < numClasses; i ++) {
            String classname = "my_generated_class" + i;
            int numOopFields = randomize ? r.nextInt(numMaxOopFields) : (i % numMaxOopFields);
            numOopFields = Math.max(1, numOopFields);
            String source = generateSource(numOopFields);
            String source0 = source.replace("CLASSNAME", classname);
            Utils.createClassFromSource(classname, source0);
        }
        System.out.println();

        System.out.print("Loading " + numClasses + "...");

        InMemoryClassLoader loader = new InMemoryClassLoader("loader", null);
        Class classes[] = new Class[numClasses];

        try {
            for (int j = 0; j < numClasses; j++) {
                classes[j] = Class.forName("my_generated_class" + j, true, loader);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.gc();

        System.out.print("Creating " + numObjectsPerClass + " objects per class...");

        int numObjects = numObjectsPerClass * numClasses;
        RETAIN = new Object[numObjects];
        int idx = 0;

        Constructor ctors[] = new Constructor[numClasses];
        for (int i = 0; i < numClasses; i++) {
            Class clazz = classes[i];
            Constructor ctor = clazz.getDeclaredConstructor();
            ctors[i] = ctor;
        }

        // Create objects in heap
        for (int j = 0; j < numObjectsPerClass; j++) {
            for (int i = 0; i < numClasses; i++) {
                int classId = randomize ?
                        r.nextInt(numClasses) :
                        (i * classStride) % numClasses;
                RETAIN[idx] = ctors[classId].newInstance();
                idx++;
           }
        }
        System.out.println();

        if (printHashes) {
            int col = 0;
            for (Object o: RETAIN) {
                int i = System.identityHashCode(o);
                System.out.print(Integer.toHexString(i) + " ");
                if (++col == 16) {
                    col = 0;
                    System.out.println("");
                }
            }
        }

        // In ball-of-yarn-mode, wire up all objects and just keep the start of the thread
        // In array mode, we keep using the RETAIN array
        Object startOfBallOfYarn;
        if (ballOfYarn) {
            BALL_OF_YARN = buildBallOfYarn(RETAIN, r);
            RETAIN = null;
        }

        System.out.println("Done; will start GCs... ");

        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        for (int i = 0; i < cycles; i++) {
            System.gc();
        }

        System.out.println("After GCs... ");

        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        return 0;

    }

}
