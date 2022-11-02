package de.stuefe.repros;

import picocli.CommandLine;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "MultiThreadTest", mixinStandardHelpOptions = true,
     description = "MultiThreadTest repro.")
public class MultiThreadTest extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num-threads", "-T" },
            description = "Number of threads (default: ${DEFAULT-VALUE}).")
    int num_threads = 1000;

    @CommandLine.Option(names = { "--stack-depth", "-D" },
            description = "Stack Depth (default: ${DEFAULT-VALUE}).")
    int stackDepth = 100;

    @CommandLine.Option(names = { "--wait-time", "-w" }, defaultValue = "10",
            description = "Seconds each thread is alive.")
    int secs;

    @CommandLine.Option(names = { "--cycles", "-c" }, defaultValue = "1",
            description = "how often we repeat this.")
    int repeat;

    @CommandLine.Option(names = { "--gc" }, defaultValue = "false",
            description = "Do a gc after each cycle.")
    boolean gc_after_cycle;

    @CommandLine.Option(names = { "--autoyes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    public static void main(String... args) {
        int exitCode = new CommandLine(new de.stuefe.repros.MultiThreadTest()).execute(args);
        System.exit(exitCode);
    }

    long do_recursively(int depth) {
        // Note: prevent tco
        if (depth == stackDepth) {
            return 0;
        }
        long l = do_recursively(depth + 1);
        if (l % 2 == 0) {
            l = l + System.currentTimeMillis();
        }
        return l;
    }

    long l = 0;
    volatile boolean stop = false;
    class Sleeper extends Thread {
        @Override
        public void run() {
            try {
                l += do_recursively(0);
                while (!stop) Thread.sleep(secs * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        System.out.println("Number of threads: " + num_threads);
        System.out.println("Wait time: " + secs);
        System.out.println("Repeat count: " + repeat);
        System.out.println("Stack depth: " + stackDepth);
        System.out.println("GC after Cycle: " + gc_after_cycle);

        for (int cycle = 0; cycle < repeat; cycle ++) {

            System.out.println("Cycle: " + cycle);
            waitForKeyPress("Will start " + num_threads + " threads, wait time " + secs + "s...");

            Thread[] sleepers = new Thread[num_threads];
            int created = 0;
            try {
                for (int i = 0; i < num_threads; i++) {
                    sleepers[i] = new Sleeper();
                    // sleepers[i] = new Thread(() -> {});
                    sleepers[i].start();
                    created++;
                    if (created % (num_threads / 10) == 0) {
                        System.out.println("Created: " + created + "...");
                    }
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                waitForKeyPress("After: " + created + " threads.");
                waitForKeyPress();
            }

            waitForKeyPress("Before stopping...");

            stop = true;

            waitForKeyPress("Before joining...");

            int joined = 0;
            for (int i = 0; i < num_threads; i++) {
                try {
                    sleepers[i].join();
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
                sleepers[i] = null;
            }
            if (gc_after_cycle) {
                System.gc();
                System.gc();
            }

        }

        waitForKeyPress("Before end...");


        return 0;
    }

}
