package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import de.stuefe.repros.util.MyTestCaseBase;
import org.apache.commons.cli.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class ManyLoadersWithWildyVaryingLifespans extends MyTestCaseBase {


    public static void main(String args[]) throws Exception {
        ManyLoadersWithWildyVaryingLifespans test = new ManyLoadersWithWildyVaryingLifespans();
        test.run(args);
    }

    private static String nameClass(int number) {
        return "myclass_" + number;
    }

    private static void generateClasses(int num, int sizeFactor) {
        for (int j = 0; j < num; j++) {
            String className = nameClass(j);
            Utils.createRandomClass(className, sizeFactor);
        }
    }

    Option options_num_loaders =
            Option.builder()
                    .longOpt("num-loaders")
                    .hasArg().type(Long.class)
                    .desc("number of loaders")
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

    private void run(String[] args) throws Exception {

        Option[] options = new Option[]{
                options_num_loaders,
                options_num_classes_per_loader,
                options_size_classes};

        prolog(getClass(), args, options);

        int numLoaders = Integer.parseInt(options_num_loaders.getValue("300"));
        int classesPerLoader =
                Integer.parseInt(options_num_classes_per_loader.getValue("100"));
        int sizeFactor =
                Integer.parseInt(options_size_classes.getValue("10"));

        Random rand = new Random();

        int numClasses = numLoaders * 10;

        System.out.print("Generating " + numClasses + " in memory class files, size factor "  + sizeFactor + " ...");
        generateClasses(classesPerLoader, sizeFactor);
        System.out.print("Done.");

        LoaderHolder _loaders[] = new LoaderHolder[numLoaders];

        System.out.print("Creating " + numLoaders + " loaders ...");

        // Create loaders
        for (int ldrIdx = 0; ldrIdx < numLoaders; ldrIdx ++) {
            InMemoryClassLoader loader = new InMemoryClassLoader("myloader" + ldrIdx, null);
            // How many ticks this loader lives. I want in average one loader to die per tick.
            int lifeSpan = rand.nextInt(numLoaders);
            for (int n = 0; n < classesPerLoader; n ++) {
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
        waitForKeyPress();

        int numLoadersAlive = numLoaders;

        // Slowly release loaders. Every once in a while we stop, gc and then
        // look at Metaspace development.
        final int stopInterval = numLoadersAlive / 10;
        int nextStopAt = numLoadersAlive;
        while (numLoadersAlive > 0) {

            int numUnloaded = 0;
            for (int ldrIdx = 0; ldrIdx < numLoaders; ldrIdx ++) {
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

                waitForKeyPress();
                nextStopAt -= stopInterval;
            }

        }

        waitForKeyPress();

        System.out.println("Done");

    }



}
