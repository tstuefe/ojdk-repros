package de.stuefe.repros;

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "Leak", mixinStandardHelpOptions = true)
public class Leak implements Callable<Integer> {

    @CommandLine.Option(names = { "-T", "--threads" },
            description = "Number of threads (default: ${DEFAULT-VALUE})")
    int numThreads = 1;

    @CommandLine.Option(names = { "-N", "--objects" },
            description = "Number of objects (default: ${DEFAULT-VALUE})")
    int objects = 10000;

    @CommandLine.Option(names = { "-t", "--time" },
            description = "Time in seconds (default: ${DEFAULT-VALUE})")
    int seconds = 10;

    volatile static boolean stop = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new Leak()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Threads: " + numThreads);
        System.out.println("Objects: " + objects);
        System.out.println("Time: " + seconds + " secs");

        Runnable r = new Runnable() {
            @Override
            public void run() {
                long i = 0;
                ArrayList<String> a = new ArrayList();
                while (Leak.stop == false) {
                    a.add("iii" + i);
                    i++;
                    if ((i % 100000) == 0) {
                        a = new ArrayList<>();
                    }
                }
                System.out.println(i);
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
