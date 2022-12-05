package de.stuefe.repros;

public class LargeArray {
    public static volatile boolean[] b;
    public static volatile byte[] bt;
    public static volatile short[] s;
    public static volatile int[] ia;
    public static volatile long[] l;
    public static volatile float[] f;
    public static volatile double[] d;
    public static volatile Object[] o;
    public static void main(String[] args) throws Exception {
        System.out.println("Testing...");
        for (int i = 0; bt == null; i ++) {
            try {
                bt = new byte[Integer.MAX_VALUE - i];
                System.out.println("largest allocatable byte[]: Integer.MAX_VALUE - " + i);
            } catch (OutOfMemoryError o) {
                System.out.println("nope... " + i);
            }
        }
        bt = null;
        for (int i = 0; b == null; i ++) {
            try {
                b = new boolean[Integer.MAX_VALUE - i];
                System.out.println("largest allocatable boolean[]: Integer.MAX_VALUE - " + i);
            } catch (OutOfMemoryError o) {
            }
        }
        b = null;
        for (int i = 0; s == null; i ++) {
            try {
                s = new short[Integer.MAX_VALUE - i];
                System.out.println("largest allocatable short[]: Integer.MAX_VALUE - " + i);
            } catch (OutOfMemoryError o) {
            }
        }
        s = null;
        System.gc();
        for (int i = 0; ia == null; i ++) {
            try {
                ia = new int[Integer.MAX_VALUE - i];
                System.out.println("largest allocatable int[]: Integer.MAX_VALUE - " + i);
            } catch (OutOfMemoryError o) {
            }
        }
        ia = null;
        System.gc();
        for (int i = 0; l == null; i ++) {
            try {
                l = new long[Integer.MAX_VALUE - i];
                System.out.println("largest allocatable long[]: Integer.MAX_VALUE - " + i);
            } catch (OutOfMemoryError o) {
            }
        }
        l = null;
        System.gc();
        for (int i = 0; f == null; i ++) {
            try {
                f = new float[Integer.MAX_VALUE - i];
                System.out.println("largest allocatable float[]: Integer.MAX_VALUE - " + i);
            } catch (OutOfMemoryError o) {
            }
        }
        f = null;
        System.gc();
        for (int i = 0; d == null; i ++) {
            try {
                d = new double[Integer.MAX_VALUE - i];
                System.out.println("largest allocatable double[]: Integer.MAX_VALUE - " + i);
            } catch (OutOfMemoryError o) {
            }
        }
        d = null;
        System.gc();
        for (int i = 0; o == null; i ++) {
            try {
                o = new Object[Integer.MAX_VALUE - i];
                System.out.println("largest allocatable Object[]: Integer.MAX_VALUE - " + i);
            } catch (OutOfMemoryError o) {
            }
        }
        o = null;
    }
}
