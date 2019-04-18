package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;

import java.util.ArrayList;

public class JustLoadAndLoad {

    public static void main(String[] args) {

        int numLoaders = 3000;

        if (args.length > 0) {
            numLoaders = Integer.parseInt(args[0]);
        }

        System.out.print("Generate classes...");
        Utils.createRandomClass("my_generated_class", 100);

        System.gc();
        MiscUtils.waitForKeyPress();

        System.out.print("Loading into " + numLoaders + "loaders...");

        ArrayList<ClassLoader> loaders = new ArrayList<>();

        for (int i = 0; i < numLoaders; i++) {

            InMemoryClassLoader loader = new InMemoryClassLoader("loader" + i, null);
            loaders.add(loader);
            try {
                Class<?> clazz = Class.forName("my_generated_class", true, loader);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
        }

        MiscUtils.waitForKeyPress();


    }

}
