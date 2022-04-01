package de.stuefe.repros.process;

import de.stuefe.repros.TestCaseBase;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "MultiThreadedSpawn", mixinStandardHelpOptions = true,
        description = "MultiThreadedSpawn repro.")
public class MultiSpawn extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num-threads" },
            description = "Number of threads (default: ${DEFAULT-VALUE})")
    int num_threads = 50;

    @CommandLine.Option(names = { "--repeat-endlessly", "-r" },
            description = "let each thread endlessly repeats spawning the processes (default: ${DEFAULT-VALUE}).")
    boolean repeat_endlessly = false;

    @CommandLine.Option(names = { "--redirect-io", "-R" },
            description = "Redirect childs IO to stdin/out (default: ${DEFAULT-VALUE}).")
    boolean redirect_io = false;

    @CommandLine.Option(names = { "--autoyes", "-y" },
            description = "Autoyes.")
    boolean auto_yes = false;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" }, description = "do not wait (only with autoyes).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" }, description = "Verbose.")
    boolean verbose = false;

    @CommandLine.Parameters(index = "0..*", description = "Command line(s) to run", arity = "1..*")
    List<String> command;

    public static void main(String... args) {
        int exitCode = new CommandLine(new MultiSpawn()).execute(args);
        System.exit(exitCode);
    }

    boolean allstop = false;

    class ProcessRunner extends Thread {
        @Override
        public void run() {
            setName("ProcessRunner");
            do {
                traceVerbose("Starting " + command + "...");
                Process p = null;
                try {
                    ProcessBuilder pb = new ProcessBuilder(command);
                    if (redirect_io) {
                        pb.inheritIO();
                    }
                    p = pb.start();
                    traceVerbose("Process started (" + p.toString() + ")");
                    p.waitFor();
                    traceVerbose("Process finished (" + p.toString() + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (repeat_endlessly && !allstop);
        }
    }

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Will start " + num_threads + " process runners...");

        Thread[] runners = new Thread[num_threads];
        int created = 0;
        try {
            for (int i = 0; i < num_threads; i++) {
                runners[i] = new ProcessRunner();
                runners[i].start();
                created++;
                if (created % (num_threads / 10) == 0) {
                    System.out.println("Created: " + created + "...");
                }
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            waitForKeyPress("OOM after: " + created + " threads.");
            waitForKeyPress();
        }

        if (repeat_endlessly) {
            waitForKeyPress("All running, press key to stop.", unattendedModeWaitSecs);
            allstop = true;
        }

        waitForKeyPress("Waiting for childs to finish...");

        int joined = 0;
        for (int i = 0; i < num_threads; i++) {
            try {
                runners[i].join();
                joined++;
                if (joined % (num_threads / 10) == 0) {
                    System.out.println("Joined: " + joined + "...");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        waitForKeyPress("After joining, before GC...");

        for (int i = 0; i < num_threads; i++) {
            runners[i] = null;
        }
        System.gc();
        System.gc();

        waitForKeyPress("Before end...");

        return 0;
    }

}

