package de.stuefe.repros.metaspace;

import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import de.stuefe.repros.util.MyTestCaseBase;
import org.apache.commons.cli.Option;

import java.util.ArrayList;

/**
 * This test demonstrates the tendency of the Metaspace allocator to hold on onto
 * memory even when class loaders are unloaded.
 *
 * We create a number of class loaders, sequentially, and order them into groups
 * (generations) such that they are interleaved (loader of gen 1 is followed by
 * one of gen 2 and so on).
 *
 * Each group of loaders is released, one after the other. Because they are interleaved,
 * we have a high degree of fragmentation. Ideally the released memory should be returned
 * to the OS, but since we created high fragmentation this will not or only partly happen.
 *
 */
public class InterleavedLoaders extends MyTestCaseBase {


    public static void main(String args[]) throws Exception {
        InterleavedLoaders test = new InterleavedLoaders();
        test.run(args);
    }

    private static String nameClass(int number) {
        return "myclass_" + number;
    }

    private static void generateClasses(int num, int sizeFactor) {
        for (int j = 0; j < num; j++) {
            String className = nameClass(j);
            Utils.createRandomClass(className, sizeFactor);
            if (j % (num / 20) == 0) {
                System.out.print("*");
            }
        }
        System.out.println(".");
    }

    Option options_num_generations =
            Option.builder()
                    .longOpt("num-gens")
                    .hasArg().type(Long.class)
                    .desc("number of generations")
                    .build();

    Option options_num_loaders_per_gen =
            Option.builder()
                    .longOpt("num-loaders")
                    .hasArg().type(Long.class)
                    .desc("number of loaders per generation")
                    .build();

    Option options_num_classes_per_loader =
            Option.builder()
                    .longOpt("num-classes-per-loader")
                    .hasArg().type(Long.class)
                    .desc("number of classes per loader")
                    .build();

    Option options_repeat_count =
            Option.builder()
                    .longOpt("repeat")
                    .hasArg().type(Long.class)
                    .desc("Repeat multiple times.")
                    .build();

    Option options_size_classes =
            Option.builder()
                    .longOpt("class-size")
                    .hasArg().type(Long.class)
                    .desc("avg class size factor")
                    .build();


    class LoaderGeneration {
        ArrayList<ClassLoader> loaders = new ArrayList<>();
        ArrayList<Class> loaded_classes = new ArrayList<>();
    }

    private void loadInterleavedLoaders(LoaderGeneration[] generations, int numGenerations,
                                        int numLoadersPerGeneration, int numClassesPerLoader) throws ClassNotFoundException {
        int numLoadersTotal = numLoadersPerGeneration * numGenerations;
        for (int nloader = 0; nloader < numLoadersTotal; nloader++) {
            ClassLoader loader = new InMemoryClassLoader("myloader", null);
            int gen = nloader % numGenerations;
            generations[gen].loaders.add(loader);
            for (int nclass = 0; nclass < numClassesPerLoader; nclass++) {
                // Let it load all classes
                Class<?> clazz = Class.forName(nameClass(nclass), true, loader);
                generations[gen].loaded_classes.add(clazz);
            }
            if (nloader % (numLoadersTotal / 20) == 0) {
                System.out.print("*");
            }
        }
        System.gc();
        System.gc();
        System.out.println(".");
    }

    private void run(String[] args) throws Exception {

        Option[] options = new Option[]{
                options_num_generations,
                options_num_loaders_per_gen,
                options_num_classes_per_loader,
                options_size_classes,
                options_repeat_count
                };

        prolog(getClass(), args, options);

        int num_generations = Integer.parseInt(options_num_generations.getValue("4"));
        int num_loaders_per_gen = Integer.parseInt(options_num_loaders_per_gen.getValue("100"));
        int num_classes_per_loader =
                Integer.parseInt(options_num_classes_per_loader.getValue("100"));
        int size_classes =
                Integer.parseInt(options_size_classes.getValue("10"));
        final int repeat_count = Integer.parseInt(options_repeat_count.getValue( "0"));

        System.out.println("Generating " + num_classes_per_loader + " classes...");
        generateClasses(num_classes_per_loader, size_classes);

        LoaderGeneration[] generations = new LoaderGeneration[num_generations];
        for (int i = 0; i < num_generations; i++) {
            generations[i] = new LoaderGeneration();
        }
        System.gc();
        System.gc();

        waitForKeyPress("Will load " + num_generations +
                " generations of " + num_loaders_per_gen + " loaders each, "
                + " each loader loading " + num_classes_per_loader + " classes...", 4);

        for (int nrun = 0; nrun < repeat_count + 1; nrun++) {

            loadInterleavedLoaders(generations, num_generations, num_loaders_per_gen, num_classes_per_loader);

            waitForKeyPress("After loading...");

            // Now: free all generations, one after the other.
            // This should give us a "breathe-out" effect which should demonstrate how much memory is retained by
            // the VM after letting go of one loader generation, in the face of fragmentation.

            for (int i = 0; i < num_generations; i++) {
                waitForKeyPress("Before freeing generation " + i + "...", 4);
                generations[i].loaders.clear();
                generations[i].loaded_classes.clear();
                System.gc();
                System.gc();
                waitForKeyPress("After freeing generation " + i + ".", 4);
            }

        }

        waitForKeyPress("Done", 4);

    }

}
