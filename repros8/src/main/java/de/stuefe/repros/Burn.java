package de.stuefe.repros;

public class Burn {

    volatile static long l = 0;
    volatile static boolean stop = false;

    public static long fibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        return fibonacci(n-1) + fibonacci(n-2);
    }

    static public void main(String[] args) throws InterruptedException {

        if (args.length != 2) {
            System.out.println("Usage: Burn <num threads> <seconds>");
        }

        int numThreads = Integer.parseInt(args[0]);
        int seconds = Integer.parseInt(args[1]);

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

    }


}
