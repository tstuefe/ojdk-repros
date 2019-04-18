package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;

public class MassiveKlass {


    public static void main(String args[]) throws Exception {

        // n Loaders, each one shall load ten classes. Size factor determines the avg class size.
        // small factor of 1 will favour small chunk sizes.
        int size = 10;

        if (args.length > 0) {
            size = Integer.parseInt(args[0]);
        }

        StringBuilder bld = new StringBuilder();
        bld.append("interface MyMassiveInterface {\n");
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

        ClassLoader loader = new InMemoryClassLoader("myloader", null);

        Class<?> clazz = Class.forName("MyMassiveClass", true, loader);

        MiscUtils.waitForKeyPress("After loading, before GC");
        clazz = null;

        System.gc(); System.gc();
        MiscUtils.waitForKeyPress();

        System.out.println("Done");

    }
}
