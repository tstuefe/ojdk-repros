package de.stuefe.repros.metaspace;

import com.sun.org.apache.bcel.internal.generic.RET;
import com.sun.tools.javac.code.Attribute;
import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.TestCaseBase;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "ManyClassesManyObjectsFullGC", mixinStandardHelpOptions = true,
        description = "ManyClassesManyObjectsFullGC repro.")
public class ManyClassesManyObjectsFullGC extends TestCaseBase implements Callable<Integer> {

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
        int exitCode = new CommandLine(new ManyClassesManyObjectsFullGC()).execute(args);
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

    static Object[] RETAIN;

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        final String source = "" +
                "public class CLASSNAME {" +
                "}";

        System.out.print("Generate " + numClasses + " classes...");
        for (int i = 0; i < numClasses; i ++) {
            String classname = "my_generated_class" + i;
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

        RETAIN = new Object[numObjectsPerClass * numClasses];
        int idx = 0;

        Constructor ctors[] = new Constructor[numClasses];
        for (int i = 0; i < numClasses; i++) {
            Class clazz = classes[i];
            Constructor ctor = clazz.getDeclaredConstructor();
            ctors[i] = ctor;
        }

        for (int j = 0; j < numObjectsPerClass; j++) {
            for (int i = 0; i < numClasses; i++) {
                // From time to time alloc a String (an object from a class coming from CDS)
//                if ((i % 100) == 0) {
//                    RETAIN[idx] = new Object();
//                } else {
                    RETAIN[idx] = ctors[i].newInstance();
//                RETAIN[idx] = new Object();
                    idx++;
 //               }
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

        System.out.print("Done; will start GCs... ");

        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        for (int i = 0; i < cycles; i++) {
            System.gc();
        }

        System.out.print("After GCs... ");

        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        return 0;


    }

}
