package de.stuefe.repros.process;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class RuntimeExecSimpleTest {

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println("Usage: RuntimeExecSimpleTest <program path>");
            return;
        }

        String[] cmd = new String[args.length];
        for (int i = 0; i < args.length; i ++) {
            cmd[i] = args[i];
        }

        for (int i = 0; i < 5; i ++) {
            Process p = Runtime.getRuntime().exec(cmd);

            System.out.println("Process started.");
            p.waitFor();
            System.out.println("Process finished.");
        }
    }


}
