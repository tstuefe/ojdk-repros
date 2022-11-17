package de.stuefe.repros;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class SmallArray {
    public static volatile byte[] b;
    public static volatile byte[] b2;
    public static void main(String[] args) throws Exception {


        b = new byte[3];
        b[0] = 'A'; b[1] = 'B'; b[2] = 'C';
        b2 = new byte[11];
        for (int i = 0; i < 20; i ++) {
            if (b.length > i) b[i] = (byte)('A' + i);
            if (b2.length > i) b2[i] = (byte)('A' + i);
        }
        System.out.println();
    }
}
