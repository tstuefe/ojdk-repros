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

    @CommandLine.Option(names = { "--randseed", "-R" }, description = "Randomizer seed (default: ${DEFAULT_VALUE})")
    long randomizerSeed=12345678;

    @CommandLine.Option(names = { "--num-classes", "-C" }, description = "Number of classes (default: ${DEFAULT_VALUE})")
    int numClasses=256;

    @CommandLine.Option(names = { "--objects", "-n" }, description = "Number of objects (default: ${DEFAULT_VALUE})")
    int numObjects=1000 * 1000 * 10;

    // Realistic defaults for the distribution of various KlassKind. Note that the typical
    // distribution can vary a lot. E.g. SpecJBB seems to have a very low TAK/OAK content. Here,
    // we use measurements from Spring petclinic liveset
    static final float OAK_distribution_default = 5.0f;
    static final float TAK_distribution_default = 12.0f;
    static final float IK_distribution_default = 100.0f - TAK_distribution_default - OAK_distribution_default;

    @CommandLine.Option(names = { "--percent-TAK" }, description = "Which percentage of liveset objects should be primitive arrays (default: ${DEFAULT_VALUE})")
    float percentTAK=TAK_distribution_default;
    @CommandLine.Option(names = { "--percent-OAK" }, description = "Which percentage of liveset objects should be Object arrays (default: ${DEFAULT_VALUE})")
    float percentOAK=OAK_distribution_default;

    @CommandLine.Option(names = { "--cycles", "-c" }, description = "Number of GC cycles (default: ${DEFAULT_VALUE})")
    int cycles = 10;

    @CommandLine.Option(names = { "--print-hashes" }, description = "Print hashes (default: ${DEFAULT_VALUE})")
    boolean printHashes = false;
    @CommandLine.Option(names = { "--wire-up" }, description = "Wire up objects (default: ${DEFAULT_VALUE})")
    boolean wireUp = false;

    static Object[] RETAIN; // array mode

    static void createInterconnections(Object[] all, Random r, Class isGeneratedMarkerInterface) throws IllegalAccessException {

        // Now wire objects up. Only do this for classes that are generated (see GenerateSources)
        for (int i = 0; i < all.length; i++) {
            Object o = all[i];
            if (isGeneratedMarkerInterface.isInstance(o)) {
                Field[] fields = o.getClass().getFields();
                for (Field f : fields) {
                    if (f.getType() == java.lang.Object.class) {
                        Object target = all[r.nextInt(all.length)];
                        f.set(o, target);
                    }
                }
            } else if (o instanceof java.lang.Object[]) {
                Object[] oa = (Object[]) o;
                for (int slot = 0; slot < oa.length; slot ++) {
                    Object target = all[r.nextInt(all.length)];
                    oa[slot] = target;
                }
            }
        }

    }

    String percentOf(int what, int what100) {
        float f = ((float) what * 100.0f) / (float) what100;
        return Float.toString(f) + "%";
    }

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        System.out.println("Random Seed: " + randomizerSeed);
        System.out.println("Num Classes: " + numClasses);
        System.out.println("Num Objects: " + numObjects);

        float percentIK = 100.0f - percentOAK - percentTAK;

        System.out.println("Percent IK Objects: " +  percentIK + "%)");
        System.out.println("Percent OAK Objects: " + percentOAK + "%)");
        System.out.println("Percent TAK Objects: " +  percentTAK + "%)");

        System.out.println("Wired up: " + wireUp);
        System.out.println("Cycles: " + cycles);

        Random r = new Random(randomizerSeed);

        System.out.println("Loading " + numClasses + " pre-generated classes ...");

        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Class classes[] = new Class[numClasses];

        try {
            for (int j = 0; j < numClasses; j++) {
                classes[j] = Class.forName("GeneratedClass" + j, true, loader);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Materialize pre-generated constructors ...");

        Constructor ctors[] = new Constructor[numClasses];
        for (int i = 0; i < numClasses; i++) {
            Class clazz = classes[i];
            Constructor ctor = clazz.getDeclaredConstructor();
            ctors[i] = ctor;
        }

        System.out.println("Creating objects...");

        System.gc();

        RETAIN = new Object[numObjects];

        int numIK = 0, numOAK = 0, numTAK = 0;

        for (int i = 0; i < numObjects; i ++) {
            Object o;
            float what = ((float)r.nextInt(100 * 1000) / 1000.0f);
            if (what < percentOAK) {
                // OAK
                int randomLength = r.nextInt(6);
                o = java.lang.reflect.Array.newInstance(Object.class, randomLength);
                numOAK ++;
            } else if (what < (percentOAK + percentTAK)) {
                // TAK
                int randomLength = r.nextInt(24);
                o = java.lang.reflect.Array.newInstance(byte.class, randomLength);
                numTAK ++;
            } else {
                // IK
                // Choose random constructor
                int randomGeneratedClass = r.nextInt(numClasses);
                o = ctors[randomGeneratedClass].newInstance();
                numIK ++;
            }
            if (o == null) {
                throw new RuntimeException("weird");
            }
            RETAIN[i] = o;
            traceVerbose("Object " + i + " is " + o.getClass().getName());
        }
        System.out.println(numObjects + " Objects created");
        System.out.println(" - IK: " + numIK + " (" + percentOf(numIK, numObjects) + ")");
        System.out.println(" - OAK: " + numOAK + " (" + percentOf(numOAK, numObjects) + ")");
        System.out.println(" - TAK: " + numTAK + " (" + percentOf(numTAK, numObjects) + ")");

        // don't retain constructors
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

        // wire up
        if (wireUp) {
            System.out.println("Wiring up...");
            Class markerInterface = loader.loadClass("IsGenerated");
            createInterconnections(RETAIN, r, markerInterface);
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
