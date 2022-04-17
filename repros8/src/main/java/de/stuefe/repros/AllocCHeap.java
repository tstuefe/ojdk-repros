package de.stuefe.repros;

import picocli.CommandLine;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

@CommandLine.Command(name = "AllocCHeap", mixinStandardHelpOptions = true,
        description = "Allocate C Heap; optionally leak or peak.")
public class AllocCHeap extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num" },
            description = "Number of allocations (default: ${DEFAULT-VALUE})")
    int numAllocations = 1024*1024*64;
    int numAllocationsPerThread = -1;

    @CommandLine.Option(names = { "--size" },
            description = "Allocation size (default: ${DEFAULT-VALUE})")
    int allocationSize = 8;

    @CommandLine.Option(names = { "--notouch" },
            description = "By default, we touch allocated memory. Can be switched off with --notouch (default: ${DEFAULT-VALUE}).")
    boolean notouch = false;

    @CommandLine.Option(names = { "--allocdelay" },
            description = "Delay, in about 0.1 ns, between subsequent mallocs (default: ${DEFAULT-VALUE}).")
    int alloc_delay = 0;

    @CommandLine.Option(names = { "--freedelay" },
            description = "Delay, in about 0.1 ns, between subsequent frees (default: ${DEFAULT-VALUE}).")
    int free_delay = 0;

    enum TestType { peak, leak };
    @CommandLine.Option(names = { "--type" },
            description = "Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    TestType testType = TestType.leak;

    enum FreeShuffleFactor { peak, leak };
    @CommandLine.Option(names = { "--free-shuffle-factor" },
            description = "If --type is peak, randomize free order between 1.0 (very random) and 0.0 (in order of allocation). (default: ${DEFAULT-VALUE})")
    double freeShuffleFactor = 0.0;

    @CommandLine.Option(names = { "--size-shuffle-factor" },
            description = "Randomize allocation size (--size) by size +- (size * factor). Valid values are 0.0 - 1.0. (default: ${DEFAULT-VALUE})")
    double sizeShuffleFactor = 0.0;

    @CommandLine.Option(names = { "--threads" },
            description = "Number of allocation threads (default: ${DEFAULT-VALUE})")
    int numThreads = 1;

    @CommandLine.Option(names = { "--cycles" },
            description = "Number of repeats (default: ${DEFAULT-VALUE})")
    int numCycles = 3;

    @CommandLine.Option(names = { "--randseed" },
            description = "If not 0, fixed randomizer seed (default: ${DEFAULT-VALUE}).")
    long randseed = 0;

    @CommandLine.Option(names = { "--auto-yes", "-y" },
            description = "Autoyes.")
    boolean auto_yes = false;

    @CommandLine.Option(names = { "--waitsecs" },
            description = "If autoyes, how many seconds to wait per step (default: ${DEFAULT-VALUE}).")
    int waitsecs = 0;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" },
            description = "verbose mode.")
    boolean verbose = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new AllocCHeap()).execute(args);
        System.exit(exitCode);
    }

    static void touchMemory(Unsafe unsafe, long address, int size) {
        for(long p = address; p < (address + size); p += 4096) {
            unsafe.putLong(p, p);
        }
    }

    static Unsafe theUnsafe;

    static class PointerArray {
        final int length;
        long values[];

        // shuffleFactor: 1.0 == shuffle whole deck once
        public final void shuffle(Random rand, double shuffleFactor) {
            int numShuffle = (int)((double)length * shuffleFactor);
            for (int i = 0; i < numShuffle; i ++) {
                int pos1 = rand.nextInt(length);
                int pos2 = rand.nextInt(length);
                long p1 = values[pos1];
                long p2 = values[pos2];
                values[pos1] = p2;
                values[pos2] = p1;
            }
        }

        public PointerArray(int length) {
            this.length = length;
            values = new long[length];
        }

        int getLength() { return length; }
        void set(int pos, long p) { values[pos] = p; }
        long getAndClear(int pos) { long p = values[pos]; values[pos] = 0; return p; }

    }

    private static final long applied_deviation(long size, double deviation) {
        return (int)(Math.min((double)size * deviation, size));
    }

    class Allocator {
        PointerArray pointers;
        Random rand;

        public Allocator(long seed, int localNumAllocations) {
            pointers = new PointerArray(localNumAllocations);
            rand = new Random(seed);
        }

        int randomized_allocation_size() {
            if (sizeShuffleFactor == 0.0) {
                return allocationSize;
            } else {
                int dev = (int)applied_deviation(allocationSize, sizeShuffleFactor);
                int range = dev * 2;
                if (range == 0) {
                    return allocationSize;
                }
                int sz = rand.nextInt(range) - dev + allocationSize;
                if (sz <= 0) {
                    sz = -sz;
                }
                return sz;
            }
        }

        void allocateAll() {
            long allocated = 0;
            for (int i = 0; i < pointers.getLength(); i ++) {
                int sz = randomized_allocation_size();
                allocated += sz;
                long p = theUnsafe.allocateMemory(sz);
                if (!notouch) {
                    touchMemory(theUnsafe, p, sz);
                }
                pointers.set(i, p);
                if ((i % 10000) == 0) {
                    sleep_delay(alloc_delay);
                }
            }
            System.out.println("> " + allocated);
        }

        void shuffleAllocations() {
            pointers.shuffle(rand, freeShuffleFactor);
        }

        void freeAll() {
            for (int i = 0; i < pointers.getLength(); i ++) {
                long p = pointers.getAndClear(i);
                theUnsafe.freeMemory(p);
            }
        }
    }

    void sleep_delay(int ms) {
        try {
           Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    class AllocationWorker extends Thread {
        CyclicBarrier barrier;
        Allocator allocator;
        final int threadNum;

        public AllocationWorker(int threadNum, CyclicBarrier barrier) {
            this.barrier = barrier;
            this.threadNum = threadNum;
            long seed = randseed + threadNum; // thread local random seed depends on global seed
            allocator = new Allocator(seed, numAllocationsPerThread);
        }

        @Override
        public void run() {
            try {
                for (int cycle = 0; cycle < numCycles; cycle ++) {
                    barrier.await();
                    traceVerbose("Worker " + threadNum + " enters allocation phase.");
                    allocator.allocateAll();
                    traceVerbose("Worker " + threadNum + " finished allocation phase.");
                    allocator.shuffleAllocations();
                    traceVerbose("Worker " + threadNum + " finished shuffling.");
                    barrier.await();
                    if (testType == TestType.peak) {
                        barrier.await();
                        traceVerbose("Worker " + threadNum + " enters free phase.");
                        allocator.freeAll();
                        traceVerbose("Worker " + threadNum + " finished free phase.");
                        barrier.await();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    final static String humanReadable(long sz) {
        int K = 1024;
        int M = K * K;
        if (sz > 10 * M) {
            return Long.toString(sz / M) + " MB";
        } else if (sz > 10 * K) {
            return Long.toString(sz / K) + " KB";
        }
        return Long.toString(sz) + " bytes";
    }

    @Override
    public Integer call() throws Exception {

        initialize(verbose, auto_yes, nowait);

        if (randseed == 0) {
            randseed = System.currentTimeMillis();
        }

        numAllocationsPerThread = numAllocations / numThreads;

        if (numAllocationsPerThread == 0) {
            System.err.println("not enough allocations to fill " + numThreads + " threads.");
            System.exit(-1);
        }

        System.out.println("Type: " + testType);
        System.out.println("Number of Allocations: " + numAllocations + " (" + numAllocationsPerThread + " per Thread)");
        System.out.println("Allocation Size: " + allocationSize);
        System.out.println("Size Shuffle factor: " + sizeShuffleFactor);
        System.out.println("Free Shuffle factor: " + freeShuffleFactor);
        System.out.println("Number of threads: " + numThreads);
        System.out.println("Touch: " + !notouch);
        System.out.println("Randomizer seed: " + randseed);
        System.out.println("Alloc delay: " + alloc_delay);
        System.out.println("Free delay: " + free_delay);

        System.out.println("----");
        if (sizeShuffleFactor > 1.0 || sizeShuffleFactor < 0.0) {
            System.err.println("invalid size shuffle factor. Must be between 0.0 and 1.0.");
        } else {
            long dev = applied_deviation(numAllocations * allocationSize, sizeShuffleFactor);
            long netAllocationSizePerCycle = (long)allocationSize * numAllocations;
            System.out.println("Net allocation per cycle will be " + humanReadable(netAllocationSizePerCycle) +
                                " +- " + humanReadable(dev) + ".");
        }

        waitForKeyPress("Lets start?", 6);

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        theUnsafe = (Unsafe) f.get(null);


        Thread[] workers = new Thread[numThreads];
        CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);
        for (int i = 0; i < numThreads; i ++) {
            workers[i] = new AllocationWorker(i, barrier);
            workers[i].start();
        }

        for (int cycle = 0; cycle < numCycles; cycle ++) {
            waitForKeyPress("Cycle " + cycle + ": before allocation...", waitsecs);
            barrier.await();
            barrier.await();
            waitForKeyPress("Cycle " + cycle + ": allocation phase completed.", 0);
            if (testType == TestType.peak) {
                waitForKeyPress("Cycle " + cycle + ": before free...", 0);
                barrier.await();
                barrier.await();
                waitForKeyPress("Cycle " + cycle + ": free phase completed.", 0);
            }
        }

        waitForKeyPress("Done.");

        for (int i = 0; i < numThreads; i ++) {
            workers[i].join();
        }

        return 0;
    }

}
