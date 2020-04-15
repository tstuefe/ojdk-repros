package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "MassiveKlass", mixinStandardHelpOptions = true,
        description = "MassiveKlass repro.")
public class MassiveKlass  implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new MassiveKlass()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Option(names = { "--num-imethods" }, defaultValue = "1000",
            description = "Number of imethods.")
    int num_imethods;

    @CommandLine.Option(names = { "--num-methods" }, defaultValue = "1000",
            description = "Number of methods.")
    int num_methods;

    @CommandLine.Option(names = { "--num-members" }, defaultValue = "1000",
            description = "Number of members.")
    int num_members;

    @Override
    public Integer call() throws Exception {

        StringBuilder bld = new StringBuilder();
        bld.append("interface MyMassiveInterface {\n");
        for (int i = 0; i < num_imethods; i ++) {
            bld.append("int get_i" + i + "();\n");
        }
        bld.append("}\n");

        bld.append("public class MyMassiveClass implements MyMassiveInterface {\n");
        for (int i = 0; i < num_imethods; i ++) {
            bld.append("public int get_i" + i + "() { return " + i + "; }\n");
        }

        for (int i = 0; i < num_methods; i ++) {
            bld.append("public int foo_" + i + "(int i) { return " + i + " + i; }\n");
        }

        for (int i = 0; i < num_members; i ++) {
            // alternate between obj and pod to increase number of oop map blocks in Klass?
            if (i % 2 == 0) {
                bld.append("public Object m_" + i + ";\n");
            } else {
                bld.append("public int m_" + i + ";\n");
            }
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

        return 0;

    }

}
