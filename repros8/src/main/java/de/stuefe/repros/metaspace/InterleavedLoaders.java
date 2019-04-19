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
 * We create batches ("generations") of class loaders. Each loader is created and
 * immediately loads n classes. Loaders from different generations are created
 * interleaved, so in metaspace their chunks are placed adjacent of each other.
 *
 * Then, we release all but one batch of classes and watch how metaspace behaves.
 * Ideally it should return the released memory to the OS, but since we created
 * high fragmentation this will not or only partly happen.
 *
 * By default we create four generations. As can be monitored from the outside,
 * even when all but one generation of loaders is released again, memory will
 * be retained almost completely.
 *
 */
public class InterleavedLoaders extends MyTestCaseBase {

    private static String nameClass(int number) {
        return "myclass_" + number;
    }

    private static void generateClasses(int num, int sizeFactor) {
        for (int j = 0; j < num; j++) {
            String className = nameClass(j);
            Utils.createRandomClass(className, sizeFactor);
        }
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

    ;

    private void run(String[] args) throws Exception {

        Option[] options = new Option[]{
                options_num_generations,
                options_num_loaders_per_gen,
                options_num_classes_per_loader,
                options_size_classes};

        prolog(getClass(), args, options);

        int num_generations = Integer.parseInt(options_num_generations.getValue("4"));
        int num_loaders_per_gen = Integer.parseInt(options_num_loaders_per_gen.getValue("100"));
        int num_classes_per_loader =
                Integer.parseInt(options_num_classes_per_loader.getValue("100"));
        int size_classes =
                Integer.parseInt(options_size_classes.getValue("10"));

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
                + " each loader loading " + num_classes_per_loader + " classes...");

        // First create all generations. Interleaved.
        for (int nloader = 0; nloader < num_loaders_per_gen * num_generations; nloader++) {
            ClassLoader loader = new InMemoryClassLoader("myloader", null);
            int gen = nloader % num_generations;
            generations[gen].loaders.add(loader);
            for (int nclass = 0; nclass < num_classes_per_loader; nclass++) {
                // Let it load all classes
                Class<?> clazz = Class.forName(nameClass(nclass), true, loader);
                generations[gen].loaded_classes.add(clazz);
            }
        }

        waitForKeyPress("After loading...");

        // Now: free all generations, one after the other, until only one is left.
        // Reallocate again until all generations are full. Repeat, but each time another
        // generation is the last one.
        // This should give us a "breathe-in-and-out" effect which should demonstrate how easily
        // VM lets go of unused metaspace even when we have fragmentation.
        for (int nrun = 0; nrun < 1000; nrun++) {
            int surviving_generation = nrun % num_generations;

            for (int i = 0; i < num_generations; i++) {
                if (i != surviving_generation) {
                    waitForKeyPress("Before freeing generation " + i + "...");
                    generations[i].loaders.clear();
                    generations[i].loaded_classes.clear();
                    System.gc();
                    System.gc();
                    waitForKeyPress("After freeing generation " + i + ".");
                }
            }

            // Now re-create all generations, interleaved.
            waitForKeyPress("Before re-loading freed generations");
            for (int nloader = 0; nloader < num_loaders_per_gen * num_generations; nloader++) {
                int gen = nloader % num_generations;
                if (gen != surviving_generation) {
                    ClassLoader loader = new InMemoryClassLoader("myloader", null);
                    generations[gen].loaders.add(loader);
                    for (int nclass = 0; nclass < num_classes_per_loader; nclass++) {
                        // Let it load all classes
                        Class<?> clazz = Class.forName(nameClass(nclass), true, loader);
                        generations[gen].loaded_classes.add(clazz);
                    }
                }
            }
            System.gc();
            System.gc();
            waitForKeyPress("After re-loading freed generations ");

        }

        System.out.println("Done");

    }

    public static void main(String args[]) throws Exception {
        InterleavedLoaders test = new InterleavedLoaders();
        test.run(args);
    }
}
