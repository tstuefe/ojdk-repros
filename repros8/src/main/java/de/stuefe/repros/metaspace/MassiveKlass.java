package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import de.stuefe.repros.util.MyTestCaseBase;
import org.apache.commons.cli.Option;

public class MassiveKlass extends MyTestCaseBase {

    private void run(String[] args) throws Exception {

        Option[] options = new Option[] {
            Option.builder()
                    .longOpt("num-imethods")
                    .hasArg().type(Long.class)
                    .desc("number of interface methods")
                    .build(),
                Option.builder()
                        .longOpt("num-methods")
                        .hasArg().type(Long.class)
                        .desc("number of methods")
                        .build(),
                Option.builder()
                        .longOpt("num-members")
                        .hasArg().type(Long.class)
                        .desc("number of int member vars")
                        .build()
        };


        prolog(getClass(), args, options);

        int size = 10;
        long num_imethods =
                Long.parseLong(cmdline.getOptionValue("num-imethods", "1000"));
        long num_methods =
                Long.parseLong(cmdline.getOptionValue("num-methods", "1000"));
        long num_members =
                Long.parseLong(cmdline.getOptionValue("num-members", "1000"));

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
            bld.append("public int m_" + i + ";\n");
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

    public static void main(String args[]) throws Exception {
        MassiveKlass test = new MassiveKlass();
        test.run(args);

    }
}
