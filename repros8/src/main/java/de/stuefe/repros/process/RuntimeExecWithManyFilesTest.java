package de.stuefe.repros.process;

import de.stuefe.repros.MiscUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RuntimeExecWithManyFilesTest {

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println("Usage: RuntimeExecWithManyFilesTest <num-procs> <num-open-files> <program path>");
            return;
        }

        int num_procs = Integer.parseInt(args[0]);
        int nofiles_open = Integer.parseInt(args[1]);

        String[] cmd = new String[args.length - 2];
        for (int i = 2; i < args.length; i ++) {
            cmd[i - 2] = args[i];
        }

        System.out.println("Openeing " + nofiles_open + " files...");

        {
            // Open files and keep them open
            List<FileReader> readers = new ArrayList<FileReader>();
            for (int i = 0; i < nofiles_open; i++) {
                FileReader r = new FileReader("/proc/meminfo");
                readers.add(r);
            }

            MiscUtils.waitForKeyPress("Before forking");

            System.out.println("Starting " + num_procs + "Processes...");
            Process[] processes = new Process[num_procs];
            for (int i = 0; i < num_procs; i++) {
                processes[i] = Runtime.getRuntime().exec(cmd);
            }
            System.out.println("Waiting on " + num_procs + "Processes...");
            for (int i = 0; i < num_procs; i++) {
                processes[i].waitFor();
            }
        }
        MiscUtils.waitForKeyPress("Child processes finished, before gc");
        System.gc();
        System.gc();
        MiscUtils.waitForKeyPress("after gc");

    }


}
