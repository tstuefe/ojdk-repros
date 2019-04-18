package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;

public class ClassLifeCycle {

    public static void main(String args[]) throws Exception {

        int numClasses = 50;
        int sizeFactor = 10;

        if (args.length > 0) {
            numClasses = Integer.parseInt(args[0]);
            if (args.length > 1) {
                sizeFactor = Integer.parseInt(args[1]);
            }
        }

        System.out.print("Generating " + numClasses + " in memory class files, size factor "  + sizeFactor + " ...");
        for (int j = 0; j < numClasses; j++) {
            String className = "testclass_" + j;
            Utils.createRandomClass(className, sizeFactor);
        }

        MiscUtils.waitForKeyPress("Before loading...");

        ClassLoader loader = new InMemoryClassLoader("myloader", null);

        for (int i = 0; i < numClasses; i ++) {
            // Loading classes...
            Class<?> clazz = Class.forName("testclass_" + i, true, loader);
        }

        MiscUtils.waitForKeyPress("After loading");

        loader = null;

        MiscUtils.waitForKeyPress("Before gc");

        System.gc(); System.gc();
        MiscUtils.waitForKeyPress("After gc");

    }


}
