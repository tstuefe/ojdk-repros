package de.stuefe.repros.metaspace;

import de.stuefe.repros.TestCaseBase;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;


import java.util.LinkedList;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "BreatheInBreatheOut", mixinStandardHelpOptions = true,
        description = "BreatheInBreatheOut repro.")
public class BreatheInBreatheOut extends TestCaseBase implements Callable<Integer> {

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
        int exitCode = new CommandLine(new BreatheInBreatheOut()).execute(args);
        System.exit(exitCode);
    }

	private static String nameClass(int number) {
        return "myclass_" + number;
    }

	private static void generateClasses(int numClasses, int sizeFactor) {
        for (int i = 0; i < numClasses; i++) {
            String className = nameClass(i);
            Utils.createRandomClass(className, sizeFactor);
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
        }
    }

    @CommandLine.Option(names = { "--num-small-loaders" }, defaultValue = "3000",
            description = "Number of small loaders.")
    int numSmallLoaders;

    @CommandLine.Option(names = { "--num-classes-per-small-loader" }, defaultValue = "1",
            description = "Number of classes per small loader.")
    int numClassesPerSmallLoader;

    @CommandLine.Option(names = { "--num-large-loaders" }, defaultValue = "1",
            description = "Number of large loaders.")
    int numLargeLoaders;

    @CommandLine.Option(names = { "--num-classes-per-large-loader" }, defaultValue = "3000",
            description = "Number of classes per large loader.")
    int numClassesPerLargeLoader;

    @CommandLine.Option(names = { "--class-size" }, defaultValue = "10",
            description = "Class size factor.")
    int sizeFactorClasses;

    @Override
    public Integer call() throws Exception {

        initialize(verbose, auto_yes, nowait);


        int num_to_generate = Math.max(numClassesPerLargeLoader, numClassesPerSmallLoader);
        System.out.print("Generate " + num_to_generate + " in memory class files...");
        generateClasses(num_to_generate, sizeFactorClasses);

        System.gc();
        System.gc();
        waitForKeyPress("Before breathing in...");

        for (int run = 0; run < 1000; run ++) {

            LinkedList<ClassLoader> loaders = new LinkedList<ClassLoader>();

            boolean do_small_loaders = (run % 2 == 0);

            int num_loaders =
                    do_small_loaders ? numSmallLoaders : numLargeLoaders;
            int num_classes_per_loader =
                    do_small_loaders ? numClassesPerSmallLoader : numClassesPerLargeLoader;

            System.out.print("Load " + numClassesPerSmallLoader + " classes into " + numSmallLoaders + " loaders...");
	        for (int nloader = 0; nloader < num_loaders; nloader ++) {
                InMemoryClassLoader loader = new InMemoryClassLoader(
                        (do_small_loaders ? "small" : "large") + "-loader-" + nloader,
                        null);
                loaders.add(loader);
                for (int ncls = 0; ncls < num_classes_per_loader; ncls++) {
                    String className = nameClass(ncls);
                    Class<?> clazz = Class.forName(className, true, loader);
                }
            }
			System.out.println("Done.");

            waitForKeyPress("Before GC.");

			// clean all up
            loaders.clear();
			System.gc();
            System.gc();
            System.out.println("Done.");

            waitForKeyPress("after GC");

		}

        return 0;
    }

}
