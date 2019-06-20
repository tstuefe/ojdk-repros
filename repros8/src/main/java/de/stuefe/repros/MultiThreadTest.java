package de.stuefe.repros;

import de.stuefe.repros.util.MyTestCaseBase;

public class MultiThreadTest extends MyTestCaseBase {
    static int secs = 0;
    static int num_threads = 0;

    static class Sleeper extends Thread {

        @Override
        public void run() {
            try {
                Thread.sleep(secs * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        MultiThreadTest test = new MultiThreadTest();
        test.run(args);
    }


    private void run(String args[]) throws Exception {

        secs = 60;
        num_threads = 100;

        if (args.length == 0 || args.length > 2) {
            System.out.println("Usage: MultiThreadTest <num_threads> [<secs>]");
            System.exit(-1);
        }

        num_threads = Integer.parseInt(args[0]);
        if (args.length > 1) {
            secs = Integer.parseInt(args[1]);
        }

        Sleeper[] sleepers = new Sleeper[num_threads];
        for (int i = 0; i < num_threads; i ++) {
            sleepers[i] = new Sleeper();
            sleepers[i].start();
        }

        waitForKeyPress();

        for (int i = 0; i < num_threads; i ++) {
            try {
                sleepers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
