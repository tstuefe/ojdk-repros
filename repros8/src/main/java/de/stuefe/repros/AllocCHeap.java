package de.stuefe.repros;

import de.stuefe.repros.util.MemorySize;
import de.stuefe.repros.util.MemorySizeConverter;
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

    @CommandLine.Option(names = { "--scenario", "-S" },
            description = "Scenario (valid values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}).")
    Scenario scenario = Scenario.leak;

    @CommandLine.Option(names = { "--num", "-n" }, converter = MemorySizeConverter.class,
            description = "Number of allocations. Accepts g, m, k suffixes.")
    Long numAllocationsL = null;
    int numAllocations = -1;
    int numAllocationsPerThread = -1;

    @CommandLine.Option(names = { "--size", "-s" }, converter = MemorySizeConverter.class,
            description = "Allocation size. Accepts g, m, k suffixes.")
    Long allocationSizeL = null;
    long allocationSize = -1;

    enum TestType { peak, leak, partleak };
    @CommandLine.Option(names = { "--type", "-t" },
            description = "Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    TestType testType = null;

    @CommandLine.Option(names = { "--touch" }, negatable = true,
            description = "Touch allocated memory (default: true).")
    Boolean touchB = null;
    boolean touch = false;

    @CommandLine.Option(names = { "--allocdelay" },
            description = "Delay, in about 0.1 ns, between subsequent mallocs (default: ${DEFAULT-VALUE}).")
    int alloc_delay = 0;

    @CommandLine.Option(names = { "--freedelay" },
            description = "Delay, in about 0.1 ns, between subsequent frees (default: ${DEFAULT-VALUE}).")
    int free_delay = 0;

    @CommandLine.Option(names = { "--free-shuffle-factor" },
            description = "If --type is peak, randomize free order between 1.0 (very random) and 0.0 (in order of allocation). (default: ${DEFAULT-VALUE})")
    double freeShuffleFactor = 0.0;

    @CommandLine.Option(names = { "--size-shuffle-factor" },
            description = "Randomize allocation size (--size) by size +- (size * factor). Valid values are 0.0 - 1.0. (default: ${DEFAULT-VALUE})")
    double sizeShuffleFactor = 0.0;

    @CommandLine.Option(names = { "--threads", "-T" },
            description = "Number of allocation threads (default: ${DEFAULT-VALUE})")
    int numThreads = 1;

    @CommandLine.Option(names = { "--cycles", "-c" },
            description = "Number of repeats (default: ${DEFAULT-VALUE})")
    int numCycles = 5;

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

    /**
     * A handy way to specify a number of parameters in a row
     */
    enum Scenario {

        leak("leaking, finegrained", (int)MemorySize.M.value() * 64, 8, TestType.leak),
        saw("saw tooth, finegrained", (int)MemorySize.M.value() * 64, 8, TestType.peak),
        rising_saw("saw tooth, finegrained, rising (partly leaking)", (int)MemorySize.M.value() * 64, 8, TestType.partleak),
        coarse_leak("leaking, coarse grained", 8, MemorySize.M.value() * 64, TestType.leak),
        coarse_saw("saw tooth, coarse grained",8, MemorySize.M.value() * 64, TestType.peak)
        ;

        public final String description;
        public final int num;
        public final long size;
        public final TestType tt;

        Scenario(String desc, int n, long sz, TestType tt) {
            this.description = desc; this.num = n; this.size = sz; this.tt = tt;
        }

        static String describeAllValues() {
            StringBuilder builder = new StringBuilder();
            for (Scenario s : values()) {
                builder.append(s.name() + "\t: " + s.description);
            }
            return builder.toString();
        }
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new AllocCHeap()).execute(args);
        System.exit(exitCode);
    }

    static void touchMemory(Unsafe unsafe, long address, long size) {
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

        void set(int pos, long p) { values[pos] = p; }
        long getAndClear(int pos) { long p = values[pos]; values[pos] = 0; return p; }

    }

    private static final long applied_deviation(long size, double deviation) {
        return (int)(Math.min((double)size * deviation, size));
    }

    class Allocator {
        PointerArray pointers;
        int numThreadLocalAllocations;
        Random rand;

        public Allocator(long seed, int localNumAllocations) {
            numThreadLocalAllocations = localNumAllocations;
            if (testType != TestType.leak) {
                pointers = new PointerArray(numThreadLocalAllocations);
            }
            rand = new Random(seed);
        }

        long randomized_allocation_size() {
            if (sizeShuffleFactor == 0.0) {
                return allocationSize;
            } else {
                if (((long)allocationSize * sizeShuffleFactor) > Integer.MAX_VALUE) {
                    throw new RuntimeException("Allocation Size too big for shuffling");
                }
                int dev = (int)applied_deviation(allocationSize, sizeShuffleFactor);
                int range = dev * 2;
                if (range == 0) {
                    return allocationSize;
                }
                long sz = rand.nextInt((int)range) - dev + allocationSize;
                if (sz <= 0) {
                    sz = -sz;
                }
                return sz;
            }
        }

        void allocateAll() {
            long allocated = 0;
            for (int i = 0; i < numThreadLocalAllocations; i ++) {
                long sz = randomized_allocation_size();
                allocated += sz;
                long p = theUnsafe.allocateMemory(sz);
                if (touch) {
                    touchMemory(theUnsafe, p, sz);
                }
                if (testType != TestType.leak) {
                    pointers.set(i, p);
                }
                if ((i % 10000) == 0) {
                    sleep_delay(alloc_delay);
                }
            }
            traceVerbose("Allocated: " + allocated);
        }

        void shuffleAllocations() {
            pointers.shuffle(rand, freeShuffleFactor);
        }

        void freeAll() {
            for (int i = 0; i < numThreadLocalAllocations; i ++) {
                long p = pointers.getAndClear(i);
                theUnsafe.freeMemory(p);
            }
        }
        void freeSome() {
            // Only free some of the pointers (in this case, only every second)
            for (int i = 0; i < numThreadLocalAllocations; i += 2) {
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
                    barrier.await();
                    if (testType != TestType.leak) {
                        allocator.shuffleAllocations();
                        traceVerbose("Worker " + threadNum + " finished shuffling.");
                        barrier.await();
                        traceVerbose("Worker " + threadNum + " enters free phase.");
                        if (testType == TestType.peak) {
                            allocator.freeAll();
                        } else {
                            allocator.freeSome();
                        }
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

        // Honor scenario; but if explicitly specified values override the scenario
        String numAllocationFromScenario = "";
        String allocationSizeFromScenario = "";
        String testTypeFromScenario = "";
        if (numAllocationsL == null) {
            numAllocationsL = new Long(scenario.num);
            numAllocationFromScenario = " (from --scenario)";
        }
        if (allocationSizeL == null) {
            allocationSizeL = new Long(scenario.size);
            allocationSizeFromScenario = " (from --scenario)";
        }
        if (testType == null) {
            testType = scenario.tt;
            testTypeFromScenario = " (from --scenario)";
        }

        numAllocations = numAllocationsL.intValue();
        numAllocationsPerThread = numAllocations / numThreads;

        allocationSize = allocationSizeL.longValue();
        touch = (touchB != null) ? touchB.booleanValue() : true;

        traceVerbose("Verbose Mode");

        System.out.println("Scenario: " + scenario + " (" + scenario.description + ")");
        System.out.println("Type: " + testType + testTypeFromScenario);
        System.out.println("Number of Allocations: " + numAllocations + " (" + numAllocationsPerThread + " per Thread)" + numAllocationFromScenario);
        System.out.println("Allocation Size: " + allocationSize + allocationSizeFromScenario);
        System.out.println("Number of threads: " + numThreads);
        System.out.println("Cycles: " + numCycles);
        System.out.println("Touch: " + (touch ? "on" : "*OFF*"));
        System.out.println("Size Shuffle factor: " + sizeShuffleFactor);
        System.out.println("Free Shuffle factor: " + freeShuffleFactor);
        System.out.println("Randomizer seed: " + randseed);
        System.out.println("Alloc delay: " + alloc_delay);
        System.out.println("Free delay: " + free_delay);

        System.out.println("----");

        if (numAllocationsPerThread == 0) {
            System.err.println("not enough allocations to fill " + numThreads + " threads.");
            System.exit(-1);
        }

        if (sizeShuffleFactor > 1.0 || sizeShuffleFactor < 0.0) {
            System.err.println("invalid size shuffle factor. Must be between 0.0 and 1.0.");
        } else {
            long dev = applied_deviation(numAllocations * allocationSize, sizeShuffleFactor);
            long netAllocationSizePerCycle = (long)allocationSize * numAllocations;
            System.out.println("Net allocation per cycle will be " + humanReadable(netAllocationSizePerCycle) +
                                " +- " + humanReadable(dev) + ".");
        }

        if (allocationSize < 32) {
            System.out.println("Please be aware that with NMT enabled, overhead may be substantial");
        }

        if (!touch && allocationSize < 4096) {
            System.out.println("Memory will not be touched. RSS may be smaller than expected.");
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
            waitForKeyPress("Cycle " + cycle + ": before allocation...", 4);
            long t1 = System.currentTimeMillis();
            barrier.await();
            System.out.println("Cycle " + cycle + ": allocating...");
            barrier.await();
            long t2 = System.currentTimeMillis();
            waitForKeyPress("Cycle " + cycle + ": allocation phase completed (" + (t2 - t1) + " ms).",4);
            if (testType != TestType.leak) {
                waitForKeyPress("Cycle " + cycle + ": before free...", 4);
                t1 = System.currentTimeMillis();
                barrier.await();
                System.out.println("Cycle " + cycle + ": freeing...");
                barrier.await();
                t2 = System.currentTimeMillis();
                waitForKeyPress("Cycle " + cycle + ": free phase completed (" + (t2 - t1) + " ms).", 0);
            }
            waitForKeyPress("Cycle " + cycle + ": ended.", waitsecs);
        }

        waitForKeyPress("Done.");

        for (int i = 0; i < numThreads; i ++) {
            workers[i].join();
        }

        return 0;
    }

}
