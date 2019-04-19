package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.InMemoryJavaFileManager;
import de.stuefe.repros.metaspace.internals.Utils;
import de.stuefe.repros.util.MyTestCaseBase;
import org.apache.commons.cli.Option;


import java.util.LinkedList;


public class BreatheInBreatheOut extends MyTestCaseBase {

    public static void main(String[] args) throws Exception {
        BreatheInBreatheOut test = new BreatheInBreatheOut();
        test.run(args);
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

	private void run(String args[]) throws Exception {

        Option[] options = new Option[]{
                Option.builder()
                        .longOpt("num-small-loaders")
                        .hasArg()
                        .build(),
                Option.builder()
                        .longOpt("num-classes-per-small-loader")
                        .hasArg()
                        .build(),
                Option.builder()
                        .longOpt("num-large-loaders")
                        .hasArg()
                        .build(),
                Option.builder()
                        .longOpt("num-classes-per-large-loader")
                        .hasArg()
                        .build(),
                Option.builder()
                        .longOpt("class-size")
                        .hasArg()
                        .build()
        };

        prolog(getClass(), args, options);

        int numSmallLoaders = Integer.parseInt(
                cmdline.getOptionValue("num-small-loaders", "3000"));
        int numClassesPerSmallLoader = Integer.parseInt(
                cmdline.getOptionValue("num-classes-per-small-loader", "1"));
        int numLargeLoaders = Integer.parseInt(
                cmdline.getOptionValue("num-large-loaders", "1"));
        int numClassesPerLargeLoader = Integer.parseInt(
                cmdline.getOptionValue("num-classes-per-large-loader", "3000"));
        int sizeFactorClasses = Integer.parseInt(
                cmdline.getOptionValue("class-size", "10"));

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
	}

}
