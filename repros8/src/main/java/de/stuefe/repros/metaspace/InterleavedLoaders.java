package de.stuefe.repros.metaspace;

import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * This test will create an artificial Metaspace usage spike by letting multiple loaders load
 * classes. It then collects those loaders and classes to measure how Metaspace recovers from
 * a temporary spike. It repeats this once.
 *
 * The loaders are loading classes in an interleaved fashion to trigger some amount of
 * Metaspace fragmentation.
 *
 * Use with --auto-yes to automatically confirm key presses and execute the whole
 * test in unattended mode. Good to collect usage curves.
 *
 * Use with both --auto-yes and --nowait to remove all waits from the code, to measure
 * performance.
 *
 * See also: ParallelLoaders, which does the same test from multiple threads.
 *
 */

@CommandLine.Command(name = "InterleavedLoaders", mixinStandardHelpOptions = true,
        description = "InterleavedLoaders repro.")
public class InterleavedLoaders implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new InterleavedLoaders()).execute(args);
        System.exit(exitCode);
    }

    private static String nameClass(int number) {
        return "myclass_" + number;
    }

    @CommandLine.Option(names = { "--num-generations" }, defaultValue = "5",
            description = "Number of loader generations.")
    int num_generations;

    @CommandLine.Option(names = { "--num-loaders" }, defaultValue = "80",
            description = "Number of loaders per generation.")
    int num_loaders_per_generation;

    @CommandLine.Option(names = { "--num-classes" }, defaultValue = "100",
            description = "Number of classes per loader.")
    int num_classes_per_loader;

    @CommandLine.Option(names = { "--interleave" }, defaultValue = "0.333",
            description = "Interleave factor (How many classes a loader loads per load step).")
    float interleave;
    int loads_per_step;

    @CommandLine.Option(names = { "--class-size" }, defaultValue = "10",
            description = "Class size factor.")
    int class_size_factor;

    @CommandLine.Option(names = { "--auto-yes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    @CommandLine.Option(names = { "--repeat-cycles" }, defaultValue = "1",
            description = "How often repeat the wave cycle.")
    int repeat_cycles;

    @CommandLine.Option(names = { "--time-raise" }, defaultValue = "0",
            description = "Seconds raising.")
    int time_raise;

    @CommandLine.Option(names = { "--time-top" }, defaultValue = "10",
            description = "Seconds top.")
    int time_top;

    @CommandLine.Option(names = { "--time-falling" }, defaultValue = "3",
            description = "Seconds falling.")
    int time_falling;

    @CommandLine.Option(names = { "--time-bottom" }, defaultValue = "60",
            description = "Seconds bottom.")
    int time_bottom;

    @CommandLine.Option(names = { "--time-prepost" }, defaultValue = "10",
            description = "Seconds before start and after finish.")
    int time_prepost;

    @CommandLine.Option(names = { "--wiggle" }, defaultValue = "0.0",
            description = "Wiggle factor (0.0 .. 1.0f, default 0,0f).")
    float wiggle = 0;

    @CommandLine.Option(names = { "--timefactor" }, defaultValue = "1.0",
            description = "Factor to increase or decrease standard times in unattended mode (0.1 .. 10.0f).")
    float timefactor;

    void waitForKeyPress(String message, int secs) {
        secs = (int)(timefactor * (float)secs);
        secs = Integer.max(0, secs);

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

    class LoaderInfo {
        ClassLoader cl = new InMemoryClassLoader("myloader", null);
        ArrayList<Class> loaded_classes = new ArrayList<>();
    }

    LoaderInfo[] loaders;

    boolean fullyLoaded() {
        for (int n = 0; n < loaders.length; n ++) {
            if (loaders[n] == null || loaders[n].loaded_classes.size() < num_classes_per_loader) {
                return false;
            }
        }
        return true;
    }

    private void loadInterleavedLoaders() throws ClassNotFoundException {

        // This "refills" the metaspace; note that this function should not have any effect on
        // loaders which are still left over from the last loading wave.

        int total_loaders = num_loaders_per_generation * num_generations;

        // (Re)create Loaders
        for (int loader = 0; loader < total_loaders; loader ++) {
            if (loaders[loader] == null) {
                loaders[loader] = new LoaderInfo();
            }
        }

        // Load classes
        do {
            for (int loader = 0; loader < total_loaders; loader ++) {
                int loaded = loaders[loader].loaded_classes.size();
                int toLoad = Integer.min(loads_per_step, num_classes_per_loader - loaded);
                for (int nclass = 0; nclass < toLoad; nclass++) {
                    String classname = nameClass(loaded + nclass);
                    Class<?> clazz = Class.forName(classname, true, loaders[loader].cl);
                    loaders[loader].loaded_classes.add(clazz);
                }
            }
            if (time_raise > 0) {
                waitForKeyPress("Load step", time_raise);
            } else {
                System.out.print("*");
            }
        } while(!fullyLoaded());

        System.gc();
        System.gc();
        System.out.println(".");
    }

    void freeGeneration(int gen) {
        int start_offset = gen;
        for (int i = start_offset; i < loaders.length; i+= num_generations) {
            loaders[i] = null;
        }
    }

    @Override
    public Integer call() throws Exception {

        interleave = Float.max(0.001f, interleave);
        interleave = Float.min(1.0f, interleave);
        loads_per_step = (int) ((float) num_classes_per_loader * interleave);
        loads_per_step = Integer.max(1, loads_per_step);
        loads_per_step = Integer.min(num_classes_per_loader, loads_per_step);

        timefactor = Float.max(0.1f, timefactor);
        timefactor = Float.min(10.0f, timefactor);

        wiggle = Float.max(0.0f, wiggle);
        wiggle = Float.min(10.0f, wiggle);

        repeat_cycles = Integer.max(1, repeat_cycles);
        repeat_cycles = Integer.min(100, repeat_cycles);

        loaders = new LoaderInfo[num_loaders_per_generation * num_generations];

        System.out.println("Loader gens: " + num_generations + ".");
        System.out.println("Loaders per gen: " + num_loaders_per_generation + ".");
        System.out.println("Classes per loader: " + num_classes_per_loader + ".");
        System.out.println("Loads per step: " + loads_per_step + " (interleave factor " + interleave + ")");
        System.out.println("Class size: " + class_size_factor + ".");
        System.out.println("Wiggle factor: " + wiggle + ".");
        System.out.println("Time factor: " + timefactor + ".");
        System.out.println("Repeat cycles: " + repeat_cycles + ".");

        waitForKeyPress("Generate " + num_classes_per_loader + " classes...", 0);

        Utils.generateClasses(num_classes_per_loader, class_size_factor, wiggle);

        System.gc();
        System.gc();

        long start = System.currentTimeMillis();

        waitForKeyPress("Before start...", time_prepost);

        for (int cycle = 0; cycle < repeat_cycles; cycle++) {
            System.out.println("Cycle: " + cycle);

            // First spike
            loadInterleavedLoaders();

            // Wait at the top of the first spike to collect samples
            waitForKeyPress("At first spike...", time_top);

            // get rid of all but the last one
            for (int i = 0; i < num_generations - 1; i++) {
                waitForKeyPress("Before freeing generation " + i + "...", 0);
                freeGeneration(i);
                System.gc();
                //System.gc();
                waitForKeyPress("After freeing generation " + i + ".", time_falling);
            }

            waitForKeyPress("Depressed...", time_bottom);

            // This GC is just there to trigger a metaspace JFR sample on older JDKs :(
            System.gc();
            //System.gc();

        }

        // Now free all
        waitForKeyPress("Before freeing all generations ",0);
        for (int i = 0; i < num_generations; i++) {
            freeGeneration(i);
        }
        System.gc();

        waitForKeyPress("After finish...", time_prepost);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.println("Elapsed Time: " + timeElapsed + " ms");

        return 0;
    }

}
