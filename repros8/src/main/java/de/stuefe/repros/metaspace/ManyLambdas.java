package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;

import java.util.ArrayList;

public class ManyLambdas {

    public static void main(String[] args) {

        int numLoaders = 3000;
        int size = 10;

        if (args.length > 0) {
            numLoaders = Integer.parseInt(args[0]);
            if (args.length > 1) {
                size = Integer.parseInt(args[0]);
            }
        }

        StringBuilder bld = new StringBuilder();
        bld.append("class MyMassiveLambdaClass {\n");
        for (int i = 0; i < size; i ++) {
            bld.append("int get_i" + i + "();\n");
        }
        bld.append("}\n");

        bld.append("public class MyMassiveClass implements MyMassiveInterface {\n");
        for (int i = 0; i < size; i ++) {
            bld.append("public int get_i" + i + "() { return " + i + "; }\n");
        }
        bld.append("};");

        Utils.createClassFromSource("MyMassiveClass", bld.toString());

        MiscUtils.waitForKeyPress("Before loading...");


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
