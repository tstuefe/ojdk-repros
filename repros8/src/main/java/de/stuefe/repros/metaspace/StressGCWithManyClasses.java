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

    @CommandLine.Option(names = { "--generate-classes", "-G" }, description = "if specified, generates classes; otherwise assumes they are present (default: ${DEFAULT_VALUE})")
    boolean generateClasses = false;

    @CommandLine.Option(names = { "--objects", "-n" }, description = "Number of objects per class (default: ${DEFAULT_VALUE})")
    int numObjectsPerClass=50000;

    @CommandLine.Option(names = { "--cycles", "-c" }, description = "Number of GC cycles (default: ${DEFAULT_VALUE})")
    int cycles = 10;

    @CommandLine.Option(names = { "--print-hashes" }, description = "Print hashes (default: ${DEFAULT_VALUE})")
    boolean printHashes = false;

    @CommandLine.Option(names = { "--no-randomization" }, description = "Disables randomization (default: ${DEFAULT_VALUE})")
    boolean dont_randomize = false;

    boolean randomize;

    @CommandLine.Option(names = { "--class-stride" }, description = "If not --randomize: stride by which we alter the class per object (default: ${DEFAULT_VALUE})")
    int classStride = 1;

    @CommandLine.Option(names = { "--flat-mode" }, description = "If true, all live objects are in an array. If false, they are wired up randomly to a ball of yarn. (default: ${DEFAULT_VALUE})")
    boolean flatMode = false;

    boolean ballOfYarnMode;

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

    Object buildBallOfYarn(Object[] all, Random r) throws IllegalAccessException {
        // now wire objects up.
        boolean[] empties = new boolean[all.length];
        for (int i = 0; i < all.length; i++) {
            Object o = all[i];
            Field[] fields = o.getClass().getFields();
            // Wire up o0..on-2 in whatever direction. Last field points to successor, to guarantee connection with
            // o0
            for (int j = 0; j < fields.length; j++) {
                boolean lastfield = (j == fields.length - 1);
                int targetIdx = 0;
                int nextObjectIdx = (i + 1) % all.length;
                if (lastfield) {
                    targetIdx = nextObjectIdx;
                } else {
                    targetIdx = randomize ? r.nextInt(all.length) : nextObjectIdx;
                }
                fields[j].set(o, all[targetIdx]);
            }
        }

        return all[0];
    }

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        randomize = !dont_randomize;
        ballOfYarnMode = !flatMode;

        System.out.println("Num Classes: " + numClasses);
        System.out.println("Num Objects per Class: " + numObjectsPerClass);
        System.out.println("Cycles: " + cycles);
        System.out.println("randomize: " + randomize + " (disable with --no-randomization)");
        System.out.println("ballOfYarn mode: " + ballOfYarnMode + " (switch to flat mode with --flat-mode)");

        Random r = new Random(0x12345678);

        int numMinOopFields = 1;
        int numMaxOopFields = 4;

        if (generateClasses) {
            System.out.print("Generate " + numClasses + " classes...");
            for (int i = 0; i < numClasses; i ++) {
                String classname = "GeneratedClass" + i;
                int numOopFields = randomize ? r.nextInt(numMaxOopFields) : (i % numMaxOopFields);
                numOopFields = Math.max(numMinOopFields, numOopFields);
                String source = generateSource(numOopFields);
                String source0 = source.replace("CLASSNAME", classname);
                Utils.createClassFromSource(classname, source0);
            }
            System.out.println();
        }

        System.out.print("Loading " + numClasses + "...");

        ClassLoader loader = generateClasses ?
                new InMemoryClassLoader("loader", null) : ClassLoader.getSystemClassLoader();

        Class classes[] = new Class[numClasses];

        try {
            for (int j = 0; j < numClasses; j++) {
                classes[j] = Class.forName("GeneratedClass" + j, true, loader);
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

        ctors = null;

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
        if (ballOfYarnMode) {
            BALL_OF_YARN = buildBallOfYarn(RETAIN, r);
            RETAIN = null;
        }

        System.out.println("Preparatory GC...");
        System.gc();
        System.gc();

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
