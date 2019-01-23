package de.stuefe.repros;


import java.nio.ByteBuffer;


public class DBBTest {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Usage: DBBTest <num buffers> <size buffers in M>");
        }

        int numBuffers = Integer.parseInt(args[0]);
        int sizeBuffers = Integer.parseInt(args[1]) * 1024 * 1024;

        System.out.println("<press key>");
        System.in.read();
        System.out.println("Allocating " + numBuffers + " buffers a " + sizeBuffers + " bytes...");
        ByteBuffer buffers[] = new ByteBuffer[numBuffers];
        for (int i = 0; i < numBuffers; i ++) {
            ByteBuffer b = ByteBuffer.allocateDirect(sizeBuffers);
            buffers[i] = b;
        }

        System.out.println("<press key for GC>");
        System.in.read();

        for (int i = 0; i < numBuffers; i ++) {
            buffers[i] = null;
        }
        System.gc();
        System.out.println("<press key>");
        System.in.read();

    }

}
