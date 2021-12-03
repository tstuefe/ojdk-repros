package de.stuefe.repros;

import java.util.Random;

public class StringDedupLeakTest {
    static int counter = 1_000_000_000;

    private static String newString() {
        counter++;
        if (counter >= 2_000_000_000) counter = 1_000_000_000;
        return "str" + counter;
    }

    static Random r = new Random();
    static String[] strings = new String[1024*1024];
    static volatile Object sink;

    public static void main(String... args) {
        for (int c = 0; c < strings.length; c++) {
            strings[c] = newString();
        }
        for (int run = 0; run < 1000000; run++) {
            if (run % 100 == 0) {
                System.out.print("*");
            }
            for (int c = 0; c < 100; c++) {
                int i = r.nextInt(strings.length);
                strings[i] = newString();
            }
            for (int c = 0; c < 1024; c++) {
                sink = new byte[64*1024];
            }
        }
        System.out.println("");
    }
}