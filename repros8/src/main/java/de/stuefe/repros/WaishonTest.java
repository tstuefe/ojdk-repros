package de.stuefe.repros;

import java.util.ArrayList;
import java.util.List;

// https://mail.openjdk.java.net/pipermail/discuss/2021-May/005807.html
// https://stackoverflow.com/questions/67550679/creating-threads-is-slower-on-amd-systems-compared-to-intel-systems
public class WaishonTest {
    public static void main(String[] args) throws InterruptedException {
        List<Thread> startedThreads = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100_000; i++) {
            Thread t = new Thread(() -> {});
            t.start();
            startedThreads.add(t);
        }

        for (Thread t : startedThreads) {
            t.join();
        }

        System.out.println("Duration: " + (System.currentTimeMillis() - startTime));
    }
}
