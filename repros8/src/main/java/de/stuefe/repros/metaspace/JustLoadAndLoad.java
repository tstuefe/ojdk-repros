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

    @CommandLine.Option(names = { "--num-loaders" }, defaultValue = "3000")
    int numLoaders;


    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        System.out.print("Generate classes...");
        Utils.createRandomClass("my_generated_class", 100);

        System.gc();
        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        System.out.print("Loading into " + numLoaders + "loaders...");

        ArrayList<ClassLoader> loaders = new ArrayList<>();

        for (int i = 0; i < numLoaders; i++) {

            InMemoryClassLoader loader = new InMemoryClassLoader("loader" + i, null);
            loaders.add(loader);
            try {
                Class<?> clazz = Class.forName("my_generated_class", true, loader);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
        }

        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        return 0;


    }

}
