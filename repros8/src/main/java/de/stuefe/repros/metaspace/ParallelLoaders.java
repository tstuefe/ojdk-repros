package de.stuefe.repros.metaspace;

import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

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

@CommandLine.Command(name = "ParallelLoaders", mixinStandardHelpOptions = true,
        description = "ParallelLoaders repro.")
public class ParallelLoaders implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new ParallelLoaders()).execute(args);
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
    int unattendedModeWaitSecs = 4;


    void waitForKeyPress(String message, int secs) {
        if (message != null) {
            System.out.println(message);
        }
        System.out.print("<press key>");
        if (auto_yes) {
            System.out.print (" ... (auto-yes) ");
            if (secs > 0) {
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

    CyclicBarrier _gate = null;

    class LoaderThread extends Thread {

        LoaderGeneration _gen;
        int _num_classes_per_loader;
        int _num_loaders;

        public LoaderThread(LoaderGeneration _gen, int _num_loaders, int _num_classes_per_loader) {
            this._gen = _gen;
            this._num_loaders = _num_loaders;
            this._num_classes_per_loader = _num_classes_per_loader;
        }

        @Override
        public void run() {
            try {
                _gate.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            System.out.println("Los gehts...");
            for (int nloader = 0; nloader < _num_loaders; nloader++) {
                ClassLoader loader = new InMemoryClassLoader("myloader", null);
                _gen.loaders.add(loader);
                for (int nclass = 0; nclass < _num_classes_per_loader; nclass++) {
                    // Let it load all classes
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(nameClass(nclass), true, loader);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    _gen.loaded_classes.add(clazz);
                }
            }
        }
    }

    private void loadLoaders(LoaderGeneration[] generations, int numGenerations,
                             int numLoadersPerGeneration, int numClassesPerLoader) {
        Thread threads[] = new Thread[numGenerations];

        _gate = new CyclicBarrier(numGenerations + 1);

        for (int i = 0; i < numGenerations; i ++) {
            Thread t = new LoaderThread(generations[i], numLoadersPerGeneration, numClassesPerLoader);
            threads[i] = t;
            t.start();
        }

        waitForKeyPress("Will load " + numGenerations +
                " generations of " + numLoadersPerGeneration + " loaders each, "
                + " each loader loading " + numClassesPerLoader + " classes...", 4);

        try {
            _gate.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < numGenerations; i ++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.gc();

        waitForKeyPress("After loading.");

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

        loadLoaders(generations, num_clusters, num_loaders_per_cluster, num_classes_per_loader);

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

        loadLoaders(generations, num_clusters, num_loaders_per_cluster, num_classes_per_loader);

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
