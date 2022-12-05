package de.stuefe.repros;

import picocli.CommandLine;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


@CommandLine.Command(name = "MultiInflate", mixinStandardHelpOptions = true,
        description = "MultiInflate")
public class MultiInflate implements Callable<Integer> {

    @CommandLine.Option(names = { "-T", "--threads" },
            description = "Number of threads (default: ${DEFAULT-VALUE})")
    int numThreads = 1;

    public static void main(String... args) {
        int exitCode = new CommandLine(new MultiInflate()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Option(names = { "-t", "--time" },
            description = "Burn time in seconds (default: ${DEFAULT-VALUE})")
    int seconds = 10;

    @CommandLine.Option(names = { "-l", "--leak" },
            description = "Leak deflaters (default: ${DEFAULT-VALUE})")
    boolean leak = false;

    volatile static boolean stop = false;

    public static ArrayList<Object> leakage = new ArrayList<>();

    class TestThread extends Thread {
        public int runs = 0;
        private void do_it() throws UnsupportedEncodingException, DataFormatException {
            String message = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

            byte[] input = message.getBytes("UTF-8");
            byte[] output = new byte[1024];

            Deflater deflater = new Deflater();
            if (leak) {
                leakage.add(deflater);
            }
            deflater.setInput(input);
            deflater.finish();
            int compressedDataLength = deflater.deflate(output);
            if (!leak) {
                deflater.end();
            }

            Inflater inflater = new Inflater();
            if (leak) {
                leakage.add(inflater);
            }
            inflater.setInput(output, 0, compressedDataLength);
            byte[] result = new byte[1024];
            int resultLength = inflater.inflate(result);
            if (!leak) {
                inflater.end();
            }
        }

        @Override
        public void run() {
            try {
                while (!stop) {
                    do_it();
                    runs++;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Integer call() throws Exception {

        System.out.println("Running on " + numThreads + " threads for " + seconds + " seconds, leak=" + leak + ".");

        TestThread[] ts = new TestThread[numThreads];

        for (int i = 0; i < numThreads; i ++) {
            TestThread t = new TestThread();
            t.start();
            ts[i] = t;
        }

        Thread.sleep(seconds * 1000);

        stop = true;

        int total = 0;
        for (int i = 0; i < numThreads; i ++) {
            ts[i].join();
            total += ts[i].runs;
        }

        System.out.println("Total runs: " + total);

        return  0;
    }


}
