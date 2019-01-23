package de.stuefe.repros;

import java.io.*;

public class SimpleFileTest {
    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.out.println("usage: SimpleFileTest <read  bytes in m> <write bytes in m>");
            return;
        }

        long read_bytes = Long.parseLong(args[0]) * 1024 * 1024;
        long write_bytes = Long.parseLong(args[1]) * 1024 * 1024;


        System.out.println("<press key>");
        System.in.read();

        if (write_bytes > 0) {
            System.out.println("Writing " + write_bytes + " bytes to files...");
            int nwritten = 0;
            while (nwritten < write_bytes) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter("/tmp/kannweg"))) {
                    while (nwritten < write_bytes) {
                        bw.write('a');
                        nwritten++;
                    }
                }
            }

            System.out.println("<press key>");
            System.in.read();

        }

        if (read_bytes > 0) {
            System.out.println("Reading " + read_bytes + " bytes from files...");
            int nread = 0;
            while (nread < read_bytes) {
                try (BufferedReader br = new BufferedReader(new FileReader("/tmp/kannweg"))) {
                    String s;
                    while (nread < read_bytes && (s = br.readLine()) != null) {
                        nread += s.length();
                    }
                }
            }

            System.out.println("<press key>");
            System.in.read();

        }

    }
}
