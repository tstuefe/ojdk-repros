package de.stuefe.repros;

import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

@CommandLine.Command(name = "ManyFilesTest", mixinStandardHelpOptions = true,
     description = "MultiThreadTest repro.")
public class ManyFilesTest extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num-files", "-n" },
            description = "Number of files (default: ${DEFAULT-VALUE}).")
    int num_files = 100;

    @CommandLine.Option(names = { "--filesize", "-s" },
            description = "Size of each file (default: ${DEFAULT-VALUE}).")
    int file_size = 1024;

    @CommandLine.Option(names = { "--close-files", "-c" },
            description = "Number of files (default: ${DEFAULT-VALUE}).")
    boolean close_files = false;

    @CommandLine.Option(names = { "--repeat", "-r" },
            description = "how often we repeat this (default: ${DEFAULT-VALUE}).")
    int repeat = 5;


    @CommandLine.Option(names = { "--gc" },
            description = "Do a gc after each cycle (default: ${DEFAULT-VALUE}).")
    boolean gc_after_cycle = false;

    @CommandLine.Option(names = { "--autoyes", "-y" },
            description = "Autoyes (default: ${DEFAULT-VALUE}).")
    boolean auto_yes = false;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes) (default: ${DEFAULT-VALUE}).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" },
            description = "Verbose (default: ${DEFAULT-VALUE}).")
    boolean verbose = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ManyFilesTest()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        System.out.println("Number of files: " + num_files);
        System.out.println("Repeat count: " + repeat);
        System.out.println("GC after Cycle: " + gc_after_cycle);

        FileOutputStream fosa[] = new FileOutputStream[num_files];

        for (int cycle = 0; cycle < repeat; cycle ++) {

            System.out.println("Cycle: " + cycle);
            waitForKeyPress("Will open " + num_files + " files ...");

            for (int i = 0; i < num_files; i ++) {
                File f = new File("TESTFILE" + i);
                fosa[i] = new FileOutputStream(f);
            }

            waitForKeyPress("Will write " + num_files + " files ...");

            for (int i = 0; i < num_files; i ++) {
                for (int j = 0; j < file_size / 100; j++) {
                    fosa[i].write("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789".getBytes());
                }
            }

            if (close_files) {
                waitForKeyPress("Will close " + num_files + " files ...");

                for (int i = 0; i < num_files; i++) {
                    for (int j = 0; j < file_size / 100; j++) {
                        fosa[i].close();
                    }
                }
            }

            waitForKeyPress("before GC...");

            if (gc_after_cycle) {
                System.gc();
                System.gc();
            }

        }

        waitForKeyPress("Before end...");

        return 0;
    }

}
