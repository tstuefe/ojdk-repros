package de.stuefe.repros;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "MultiThreadTest", mixinStandardHelpOptions = true,
     description = "MultiThreadTest repro.")
public class MultiThreadTest extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num-threads" }, defaultValue = "1000",
            description = "Number of threads.")
    int num_threads;

    @CommandLine.Option(names = { "--wait-time" }, defaultValue = "10",
            description = "Seconds each thread is alive.")
    int secs;

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

    class Sleeper extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(secs * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Will start " + num_threads + " threads, wait time " + secs + "s...");

        Thread[] sleepers = new Thread[num_threads];
        int created = 0;
        try {
            for (int i = 0; i < num_threads; i++) {
                sleepers[i] = new Sleeper();
               // sleepers[i] = new Thread(() -> {});
                sleepers[i].start();
                created ++;
                if (created % (num_threads / 10) == 0) {
                    System.out.println("Created: " + created + "...");
                }
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            waitForKeyPress("After: " + created + " threads.");
            waitForKeyPress();
        }

        waitForKeyPress("Before joining...");

        int joined = 0;
        for (int i = 0; i < num_threads; i ++) {
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

        waitForKeyPress();

        return 0;
    }

}
