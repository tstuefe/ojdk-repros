package de.stuefe.repros.metaspace;


import de.stuefe.repros.TestCaseBase;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "ClassLifeCycle", mixinStandardHelpOptions = true)
public class ClassLifeCycle extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--auto-yes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ClassLifeCycle()).execute(args);
        System.exit(exitCode);
    }


    @CommandLine.Option(names = { "--num-classes-per-loader" }, defaultValue = "100")
    int numClasses;

    @CommandLine.Option(names = { "--class-size" }, defaultValue = "10",
            description = "Class size factor.")
    int sizeFactor;

    class LoaderGeneration {
        ArrayList<ClassLoader> loaders = new ArrayList<>();
        ArrayList<Class> loaded_classes = new ArrayList<>();
    }


    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes);

        System.out.print("Generating " + numClasses + " in memory class files, size factor "  + sizeFactor + " ...");
        for (int j = 0; j < numClasses; j++) {
            String className = "testclass_" + j;
            Utils.createRandomClass(className, sizeFactor);
        }

        waitForKeyPress("Before creating loader...");

        ClassLoader loader = new InMemoryClassLoader("myloader", null);

        waitForKeyPress("Before loading...");

        for (int i = 0; i < numClasses; i ++) {
            // Loading classes...
            Class<?> clazz = Class.forName("testclass_" + i, true, loader);
        }

        waitForKeyPress("After loading");

        loader = null;

        waitForKeyPress("Before gc");

        System.gc(); System.gc();
        waitForKeyPress("After gc");

        return 0;

    }


}
