package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;


import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "ManyLoadersWithWildyVaryingLifespans", mixinStandardHelpOptions = true,
        description = "ManyLoadersWithWildyVaryingLifespans repro.")
public class ManyLoadersWithWildyVaryingLifespans implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new ManyLoadersWithWildyVaryingLifespans()).execute(args);
        System.exit(exitCode);
    }

    private static String nameClass(int number) {
        return "myclass_" + number;
    }

    private static void generateClasses(int num, int sizeFactor, float wiggle) {
        for (int j = 0; j < num; j++) {
            String className = nameClass(j);
            Utils.createRandomClass(className, sizeFactor, wiggle);
            if (j % 100 == 0) {
                System.out.print("*");
            }
        }
        System.out.println(".");
    }


    @CommandLine.Option(names = { "--num-loaders" }, defaultValue = "300",
            description = "Number of loaders.")
    int num_loaders;

    @CommandLine.Option(names = { "--num-classes" }, defaultValue = "100",
            description = "Number of classes per loader.")
    int num_classes_per_loader;

    @CommandLine.Option(names = { "--class-size" }, defaultValue = "10",
            description = "Class size factor.")
    int class_size_factor;

    @CommandLine.Option(names = { "--wiggle" }, defaultValue = "0.0",
            description = "Wiggle factor (0.0 .. 1.0f, default 0,0f).")
    float wiggle = 0;

    class LoaderGeneration {
        ArrayList<ClassLoader> loaders = new ArrayList<>();
        ArrayList<Class> loaded_classes = new ArrayList<>();
    }

    ;

    static class LoaderHolder {
        final ClassLoader loader;
        int _lifeSpan;

        public LoaderHolder(int lifeSpan, ClassLoader loader) {
            this.loader = loader;
            this._lifeSpan = lifeSpan;
        }

        boolean tick() {
            _lifeSpan--;
            return _lifeSpan <= 0;
        }

    }

    @Override
    public Integer call() throws Exception {


        System.out.println("Loaders: " + num_loaders + ".");
        System.out.println("Classes per loader: " + num_classes_per_loader + ".");
        System.out.println("Class size: " + class_size_factor + ".");
        System.out.println("Wiggle factor: " + wiggle + ".");

        generateClasses(num_classes_per_loader, class_size_factor, wiggle);

        Random rand = new Random();

        LoaderHolder _loaders[] = new LoaderHolder[num_loaders];

        System.out.print("Creating " + num_loaders + " loaders ...");

        // Create loaders
        for (int ldrIdx = 0; ldrIdx < num_loaders; ldrIdx ++) {
            InMemoryClassLoader loader = new InMemoryClassLoader("myloader" + ldrIdx, null);
            // How many ticks this loader lives. I want in average one loader to die per tick.
            int lifeSpan = rand.nextInt(num_loaders);
            for (int n = 0; n < num_classes_per_loader; n ++) {
                String className = nameClass(n);
                Class<?> clazz = Class.forName(className, true, loader);
            }
            _loaders[ldrIdx] = new LoaderHolder(lifeSpan, loader);
            if (ldrIdx % 250 == 0) {
                System.out.println(ldrIdx + "...");
            }
        }

        // Clean up class generation remnants.
        System.gc();
        System.gc();
        MiscUtils.waitForKeyPress();

        int numLoadersAlive = num_loaders;

        // Slowly release loaders. Every once in a while we stop, gc and then
        // look at Metaspace development.
        final int stopInterval = numLoadersAlive / 10;
        int nextStopAt = numLoadersAlive;
        while (numLoadersAlive > 0) {

            int numUnloaded = 0;
            for (int ldrIdx = 0; ldrIdx < num_loaders; ldrIdx ++) {
                if (_loaders[ldrIdx] != null && _loaders[ldrIdx].tick()) {
                    _loaders[ldrIdx] = null;
                    numUnloaded ++;
                }
            }
            numLoadersAlive -= numUnloaded;

            if (numLoadersAlive <= nextStopAt) {
                // clean all up
                System.gc();
                System.out.println("Alive: " + numLoadersAlive);
                de.stuefe.repros.process.Utils.executeCommand("/bin/ps", "-o", "pid,rss", "" + de.stuefe.repros.process.Utils.getPid());

                String jcmdBinary = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "jcmd";
                de.stuefe.repros.process.Utils.executeCommand(jcmdBinary, "" + de.stuefe.repros.process.Utils.getPid(), "VM.metaspace", "basic", "scale=k");

                MiscUtils.waitForKeyPress();
                nextStopAt -= stopInterval;
            }

        }

        MiscUtils.waitForKeyPress();

        System.out.println("Done");

        return 0;
    }



}
