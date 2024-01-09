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


    static Object[] RETAIN;

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        System.out.print("Generate " + numClasses + " classes...");
        for (int i = 0; i < numClasses; i ++) {
            Utils.createRandomClass("my_generated_class" + i, 1);
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

        for (int i = 0; i < numClasses; i ++) {
            Class clazz = classes[i];
            Constructor ctor = clazz.getDeclaredConstructor();
            for (int j = 0; j < numObjectsPerClass; j++) {
                RETAIN[idx] = ctor.newInstance();
                idx++;
            }
            System.out.print("*");
        }
        System.out.println();

        System.out.print("Done; will start GCs... ");

        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        for (int i = 0; i < 20; i++) {
            System.gc();
        }

        System.out.print("After GCs... ");

        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        return 0;


    }

}
