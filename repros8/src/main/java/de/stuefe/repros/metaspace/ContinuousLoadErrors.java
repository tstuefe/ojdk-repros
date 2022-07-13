package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.TestCaseBase;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "ContinuousLoadErrors", mixinStandardHelpOptions = true,
        description = "ContinuousLoadErrors repro.")
public class ContinuousLoadErrors extends TestCaseBase implements Callable<Integer> {

    // Mimic the case where client code tries to continuously load a class and fails.

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

    @CommandLine.Option(names = { "--num-loaders", "-n" }, description = "Number of loaders (default: $DEFAULT-VALUE)")
    int numLoaders = 3000;

    @CommandLine.Option(names = { "--keep-loaders", "-k" }, description = "Prevent loaders from being GC'd")
    boolean keeploaders = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ContinuousLoadErrors()).execute(args);
        System.exit(exitCode);
    }

    static class MyBrokenClassLoader extends ClassLoader {

        public MyBrokenClassLoader(String name, ClassLoader parent) {
            // 1.9 ++ but we want to keep 1.8 compatibility
            // super(name, parent);
            super(parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }
    }


    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        System.gc();
        if (!nowait) {
            MiscUtils.waitForKeyPress();
        }

        System.out.print("Loading into " + numLoaders + "loaders...");

        ArrayList<ClassLoader> loaders = new ArrayList<>();

        for (int i = 0; i < numLoaders; i++) {
            MyBrokenClassLoader loader = new MyBrokenClassLoader("loader" + i, null);
            if (keeploaders) {
                loaders.add(loader);
            }
            boolean got_error = false;
            try {
                Class<?> clazz = Class.forName("gibsnicht", true, loader);
            } catch (ClassNotFoundException e) {
                got_error = true;
            }
            if (!got_error) {
                System.out.println("Did not get an error as expected?");
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
