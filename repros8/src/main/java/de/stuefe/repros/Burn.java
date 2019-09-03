package de.stuefe.repros;

import picocli.CommandLine;

import java.util.concurrent.Callable;


@CommandLine.Command(name = "Burn", mixinStandardHelpOptions = true,
        description = "Just burn cpu on a number of threads.")
public class Burn implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "Number of threads.")
    int numThreads = 1;

    @CommandLine.Parameters(index = "1", description = "Number of threads.")
    int seconds = 10;

    volatile static long l = 0;
    volatile static boolean stop = false;

    public static long fibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        return fibonacci(n-1) + fibonacci(n-2);
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Burn()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        System.out.println("Burning on " + numThreads + " threads for " + seconds + " seconds.");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (Burn.stop == false) {
                    Burn.l = Burn.fibonacci(20);
                }
            }
        };

        Thread[] ts = new Thread[numThreads];
        for (int i = 0; i < numThreads; i ++) {
            Thread t = new Thread(r);
            t.start();
            ts[i] = t;
        }

        Thread.sleep(seconds * 1000);

        stop = true;
        for (int i = 0; i < numThreads; i ++) {
            ts[i].join();
        }

        return  0;
    }


}
