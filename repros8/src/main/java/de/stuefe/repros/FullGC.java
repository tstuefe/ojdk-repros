package de.stuefe.repros;

public class FullGC {
    static final int COUNT = 100_000_000;
    static final Object[] RETAIN = new Object[COUNT];

    public static void main(String... args) throws Throwable {
        for (int c = 0; c < COUNT; c++) {
            RETAIN[c] = new Object();
        }

        for (int c = 0; c < 20; c++) {
            System.gc();
        }
    }
}
