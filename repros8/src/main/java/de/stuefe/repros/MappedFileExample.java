package de.stuefe.repros;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedFileExample
{
    static int length = 0x8FFFFFF; // 128 Mb

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: MappedFileExample <num files> <file size in mb>");
            return;
        }
        int numfiles = Integer.parseInt(args[0]);
        long fileSize = Long.parseLong(args[1]) * 0x100000;

        System.out.println("<press key>");
        System.in.read();
        System.out.println("allocating...");
        MappedByteBuffer[] mappings = new MappedByteBuffer[numfiles];
        for (int i = 0; i < numfiles; i ++) {
            String path = "/tmp/MappedFileExample_" + i + ".tmp";
            MappedByteBuffer mb = new RandomAccessFile(path, "rw")
                    .getChannel().map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            mappings[i] = mb;
            for (long pos = 0; pos < fileSize; pos += 1) {
                mb.put((byte)17);
            }
        }
        System.out.println("<press key>");
        System.in.read();
    }
}
