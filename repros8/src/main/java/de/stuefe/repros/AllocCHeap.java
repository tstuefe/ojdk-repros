package de.stuefe.repros;

import picocli.CommandLine;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "AllocCHeap", mixinStandardHelpOptions = true,
        description = "Allocate C Heap; optionally leak or peak.")
public class AllocCHeap extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--size" }, defaultValue = "8",
            description = "Allocation size (default: ${DEFAULT-VALUE})")
    int allocationSize = 8;

    @CommandLine.Option(names = { "--num" },
            description = "Number of allocations (default: ${DEFAULT-VALUE})")
    int numAllocations = 1024*1024*64;

    @CommandLine.Option(names = { "--cycles" },
            description = "Number of repeats (default: ${DEFAULT-VALUE})")
    int numCycles = 3;

    @CommandLine.Option(names = { "--touch" },
            description = "Touch touch? (default: ${DEFAULT-VALUE})")
    boolean touch = true;

    enum TestType { peak, leak };
    @CommandLine.Option(names = { "--type" }, defaultValue = "peak",
            description = "Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    TestType testType;

    @CommandLine.Option(names = { "--noise" },
            description = "A \"noise level\" indicator - if > 0, executes lots of malloc/free calls concurrently to " +
                          "stress the allocation layer (default: ${DEFAULT-VALUE})")
    int noise = 0;

    public static void main(String... args) {
        int exitCode = new CommandLine(new AllocCHeap()).execute(args);
        System.exit(exitCode);
    }

    static void touchMemory(Unsafe unsafe, long address, int size) {
        for(long p = address; p < (address + size); p += 4096) {
            unsafe.putByte(p, (byte)1);
        }
    }

    static boolean stopnoise = false;
    static Unsafe theUnsafe;
    static Random random = new Random();

    class NoiseThread extends Thread {

        @Override
        public void run() {
            long[] p = new long[10];
            while(!stopnoise) {
                for (int i = 0; i < p.length; i ++) {
                    p[i] = theUnsafe.allocateMemory(random.nextInt(30));
                }
                for (int i = 0; i < p.length; i ++) {
                    theUnsafe.freeMemory(p[i]);
                }
            }
        }
    }

    @Override
    public Integer call() throws Exception {

        System.out.println("Number of Cylces: " + numCycles);
        System.out.println("Number of Allocations: " + numAllocations);
        System.out.println("Allocation Size: " + allocationSize);
        System.out.println("Touch: " + touch);
        System.out.println("Type: " + testType);
        System.out.println("Noise: " + noise);

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        theUnsafe = (Unsafe) f.get(null);

        long[] ptrs = new long[numAllocations];
        Thread[] noiseThreads = new Thread[noise];

        if (noise > 0) {
            for (int i = 0; i < noise; i ++) {
                noiseThreads[i] = new NoiseThread();
                noiseThreads[i].start();
            }
        }

        for (int cycle = 0; cycle < numCycles; cycle ++) {

            waitForKeyPress("Cycle " + cycle + ": before allocation...");

            for (int n = 0; n < numAllocations; n++) {
                long p = theUnsafe.allocateMemory(allocationSize);
                ptrs[n] = p;
                if (touch) {
                    touchMemory(theUnsafe, p, allocationSize);
                }
            }

            waitForKeyPress("Cycle " + cycle + ": allocation phase completed.");

            if (testType == TestType.peak) {
                waitForKeyPress("Cycle " + cycle + ": before free...");
                for (int n = 0; n < numAllocations; n++) {
                    theUnsafe.freeMemory(ptrs[n]);
                    ptrs[n] = 0;
                }
                waitForKeyPress("Cycle " + cycle + ": free phase completed.");
            }
        }

        waitForKeyPress("Done.");

        stopnoise = true;
        if (noise > 0) {
            for (int i = 0; i < noise; i ++) {
                noiseThreads[i].join();
            }
        }

        return 0;
    }

}
