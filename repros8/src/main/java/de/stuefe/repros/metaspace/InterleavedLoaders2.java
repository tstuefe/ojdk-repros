package de.stuefe.repros.metaspace;

import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

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

@CommandLine.Command(name = "InterleavedLoaders", mixinStandardHelpOptions = true,
        description = "InterleavedLoaders repro.")
public class InterleavedLoaders2 implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new InterleavedLoaders2()).execute(args);
        System.exit(exitCode);
    }

    private static String nameClass(int number) {
        return "myclass_" + number;
    }

    private static void generateClasses(int num, int sizeFactor) {
        for (int j = 0; j < num; j++) {
            String className = nameClass(j);
            Utils.createRandomClass(className, sizeFactor);
            if (j % 100 == 0) {
                System.out.print("*");
            }
        }
        System.out.println(".");
    }

    @CommandLine.Option(names = { "--num-clusters" }, defaultValue = "5",
            description = "Number of loader clusters.")
    int num_clusters;

    @CommandLine.Option(names = { "--num-loaders" }, defaultValue = "80",
            description = "Number of loaders per cluster.")
    int num_loaders_per_cluster;

    @CommandLine.Option(names = { "--num-classes" }, defaultValue = "100",
            description = "Number of classes per loader.")
    int num_classes_per_loader;

    @CommandLine.Option(names = { "--class-size" }, defaultValue = "10",
            description = "Class size factor.")
    int class_size_factor;

    @CommandLine.Option(names = { "--auto-yes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    int unattendedModeWaitSecs = 4;


    void waitForKeyPress(String message, int secs) {
        if (message != null) {
            System.out.println(message);
        }
        System.out.print("<press key>");
        if (auto_yes) {
            System.out.print (" ... (auto-yes) ");
            if (secs > 0 && nowait == false) {
                System.out.print("... waiting " +secs + " secs ...");
                try {
                    Thread.sleep(secs * 1000);
                } catch (InterruptedException e) {
                }
            }
        } else {
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println (" ... continuing.");
    }

    void waitForKeyPress(String message) { waitForKeyPress(message, unattendedModeWaitSecs); }

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


    @Override
    public Integer call() throws Exception {

        System.out.println("Loader clusters: " + num_clusters + ".");
        System.out.println("Loaders per cluster: " + num_loaders_per_cluster + ".");
        System.out.println("Classes per loader: " + num_classes_per_loader + ".");
        System.out.println("Class size factor: " + class_size_factor + ".");

        generateClasses(num_classes_per_loader, class_size_factor);

        LoaderGeneration[] generations = new LoaderGeneration[num_clusters];
        for (int i = 0; i < num_clusters; i++) {
            generations[i] = new LoaderGeneration();
        }
        System.gc();
        System.gc();

        waitForKeyPress("Will load " + num_clusters +
                " generations of " + num_loaders_per_cluster + " loaders each, "
                + " each loader loading " + num_classes_per_loader + " classes...", 4);

        loadInterleavedLoaders(generations, num_clusters, num_loaders_per_cluster, num_classes_per_loader);

        waitForKeyPress("After loading...");

        // get rid of all but the last two
        for (int i = num_clusters - 1; i >= 1; i--) {
            waitForKeyPress("Before freeing generation " + i + "...");
            generations[i].loaders.clear();
            generations[i].loaded_classes.clear();
            System.gc();
            System.gc();
            waitForKeyPress("After freeing generation " + i + ".");
        }

        waitForKeyPress(null, 60);

        loadInterleavedLoaders(generations, num_clusters, num_loaders_per_cluster, num_classes_per_loader);

        waitForKeyPress("After loading.");

        // Now free all
        for (int i = num_clusters - 1; i >= 0; i--) {
            waitForKeyPress("Before freeing generation " + i + "...");
            generations[i].loaders.clear();
            generations[i].loaded_classes.clear();
            System.gc();
            System.gc();
            waitForKeyPress("After freeing generation " + i + ".");
        }

        return 0;
    }

}