package de.stuefe.repros;

import java.io.IOException;

public class RuntimeExecSimpleTest {

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println("Usage: RuntimeExecSimpleTest <program path>");
            return;
        }

        String[] cmd = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            cmd[i] = args[i];
        }

        for (int i = 0; i < 5; i++) {
            try {
                Process p = Runtime.getRuntime().exec(cmd);
                ProcessHandle ph = p.toHandle();
                System.out.println(ph.toString() + " - " + ph.info());

                System.out.println("Process started.");
                p.waitFor();
                System.out.println("Process finished (exitcode " + p.exitValue() + ")");
            } catch (IOException e) {
                System.out.println("Error: " + e.toString());
                e.printStackTrace();
            }
        }
    }


}
