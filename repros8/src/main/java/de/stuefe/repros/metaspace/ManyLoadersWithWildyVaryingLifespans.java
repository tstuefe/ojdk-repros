package de.stuefe.repros.metaspace;

import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;

import java.util.Random;

public class ManyLoadersWithWildyVaryingLifespans {

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

    private static String nameClass(int number) {
        return "myclass_" + number;
    }

    private static void generateClasses(int num, int sizeFactor) {
        for (int j = 0; j < num; j++) {
            String className = nameClass(j);
            Utils.createRandomClass(className, sizeFactor);
        }
    }

    public static void main(String args[]) throws Exception {

        Random rand = new Random();

        // n Loaders, each one shall load ten classes. Size factor determines the avg class size.
        // small factor of 1 will favour small chunk sizes.
        int numLoaders = 3000;
        int sizeFactor = 1;
        int classesPerLoader = 10;

        if (args.length > 0) {
            numLoaders = Integer.parseInt(args[0]);
            if (args.length > 1) {
                sizeFactor = Integer.parseInt(args[1]);
            }
        }

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
//        System.out.println("<press key>");
//        System.in.read();

        int numLoadersAlive = numLoaders;

        // tick tock
        // Every nth tick we stop and let the user take a look.
        final int stopInterval = numLoadersAlive / 10;
        int nextStopAt = numLoadersAlive;
        while (numLoadersAlive > 0) {

            if (numLoadersAlive <= nextStopAt) {
                // clean all up
                System.gc();
                System.out.println("Alive: " + numLoadersAlive);
                de.stuefe.repros.process.Utils.executeCommand("/bin/ps", "-o", "pid,rss", "" + de.stuefe.repros.process.Utils.getPid());
                de.stuefe.repros.process.Utils.executeCommand("/shared/projects/openjdk/jdks/openjdk11/bin/jcmd", "" + de.stuefe.repros.process.Utils.getPid(), "VM.metaspace", "basic", "scale=k");
//                System.out.println("<press key>");
//                System.in.read();
                nextStopAt -= stopInterval;
            }

            int numUnloaded = 0;
            for (int ldrIdx = 0; ldrIdx < numLoaders; ldrIdx ++) {
                if (_loaders[ldrIdx] != null && _loaders[ldrIdx].tick()) {
                    // unload this guy
                    _loaders[ldrIdx] = null;
                    numUnloaded ++;
                }
            }
            numLoadersAlive -= numUnloaded;

        }
        System.out.println("Done");

    }



}
