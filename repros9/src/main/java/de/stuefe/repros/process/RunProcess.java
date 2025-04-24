package de.stuefe.repros.process;

import java.io.IOException;

public class RunProcess {
    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println("Usage: RunProcess <program> <args>");
            return;
        }

        String[] cmd = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            cmd[i] = args[i];
        }

        for (int i = 0; i < 5; i++) {
            try {
                ProcessBuilder bld = new ProcessBuilder().command(cmd).inheritIO();
                Process p = bld.start();
                p.waitFor();
                System.out.println("Process finished (exitcode " + p.exitValue() + ")");
            } catch (IOException e) {
                System.out.println("Error: " + e.toString());
                e.printStackTrace();
            }
        }
    }


}
