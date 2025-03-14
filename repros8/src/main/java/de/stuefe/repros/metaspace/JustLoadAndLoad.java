package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.TestCaseBase;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "JustLoadAndLoad", mixinStandardHelpOptions = true,
        description = "JustLoadAndLoad repro.")
public class JustLoadAndLoad extends TestCaseBase implements Callable<Integer> {

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
        int exitCode = new CommandLine(new JustLoadAndLoad()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Option(names = { "--num-classes", "-C" }, description = "Number of classes (default: ${DEFAULT_VALUE})")
    int numClasses=3000;

    @CommandLine.Option(names = { "--num-loaders", "-L" }, description = "Number of classes (default: ${DEFAULT_VALUE})")
    int numLoaders=1;

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        System.out.print("Generate " + numClasses + " classes...");
        for (int i = 0; i < numClasses; i ++) {
            Utils.createRandomClass("my_generated_class" + i, 1);
            if ((i % 1000) == 0) {
                System.out.print("*");
            }
        }
        System.out.println();

        System.gc();
        waitForKeyPress("Done loading clases");

        System.out.print("Loading " + numClasses + " into " + numLoaders + "loaders...");

        ClassLoader[] loaders = new ClassLoader[numLoaders];

        for (int i = 0; i < numLoaders; i++) {

            InMemoryClassLoader loader = new InMemoryClassLoader("loader" + i, null);
            try {
                for (int j = 0; j < numClasses; j++) {
                    Class<?> clazz = Class.forName("my_generated_class" + j, true, loader);
                }
                loaders[i] = loader;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
        }

        waitForKeyPress("Done");

        return 0;


    }

}
