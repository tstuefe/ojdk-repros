package de.stuefe.repros;

import picocli.CommandLine;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "AllocCHeap", mixinStandardHelpOptions = true,
        description = "Allocate C Heap; optionally leak or peak.")
public class AllocCHeap extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--auto-yes", "-y" },
            description = "Autoyes.")
    boolean auto_yes = false;

    @CommandLine.Option(names = { "--waitsecs" },
            description = "If autoyes, how many seconds to wait per step (default: ${DEFAULT-VALUE}).")
    int waitsecs = 0;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--size" },
            description = "Allocation size (default: ${DEFAULT-VALUE})")
    int allocationSize = 8;

    @CommandLine.Option(names = { "--randfactor" },
            description = "Randomize size between [size..size * randfactor) (default: ${DEFAULT-VALUE})")
    int randfactor = 1;

    @CommandLine.Option(names = { "--randseed" },
            description = "If not 0, fixed randomizer seed (default: ${DEFAULT-VALUE}).")
    long randseed = 0;

    @CommandLine.Option(names = { "--num" },
            description = "Number of allocations (default: ${DEFAULT-VALUE})")
    int numAllocations = 1024*1024*64;

    @CommandLine.Option(names = { "--allocdelay" },
            description = "Delay, in about 0.1 ns, between subsequent mallocs (default: ${DEFAULT-VALUE}).")
    int alloc_delay = 0;

    @CommandLine.Option(names = { "--freedelay" },
            description = "Delay, in about 0.1 ns, between subsequent frees (default: ${DEFAULT-VALUE}).")
    int free_delay = 0;

    @CommandLine.Option(names = { "--cycles" },
            description = "Number of repeats (default: ${DEFAULT-VALUE})")
    int numCycles = 3;

    @CommandLine.Option(names = { "--notouch" },
            description = "By default, we touch allocated memory. Can be switched off with --notouch (default: ${DEFAULT-VALUE}).")
    boolean notouch = false;

    enum TestType { peak, leak };
    @CommandLine.Option(names = { "--type" },
            description = "Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    TestType testType = TestType.leak;

    enum FreeType { inorder, interleaved };
    @CommandLine.Option(names = { "--freetype" },
            description = "Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    FreeType freeType = FreeType.interleaved;

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
            unsafe.putLong(p, p);
        }
    }

    class Allocator {
        long p[];
        Random rand;
        int freed;
        public Allocator(int num, long seed) {
            p = new long[num];
            rand = new Random(seed);
            int freed = 0;
        }

        void allocateAll() {
            for (int i = 0; i < p.length; i ++) {
                int sz = randomized_allocation_size();
                p[i] = theUnsafe.allocateMemory(sz);
                if (!notouch) {
                    touchMemory(theUnsafe, p[i], sz);
                }
                if ((i % 10000) == 0) {
                    sleep_delay(alloc_delay);
                }
            }
        }

        void freeInOrder(int numToFree) {
            for (int i = freed; i < numToFree; i ++) {
                if (p[i] != 0) {
                    theUnsafe.freeMemory(p[i]);
                    p[i] = 0;
                    if ((i % 10000) == 0) {
                        sleep_delay(free_delay);
                    }
                }
            }
            freed += numToFree;
        }

        void freeInterleaved(int numToFree) {
            for (int offs = 0; offs < 2; offs ++) {
                for (int i = offs; i < numToFree; i += 2) {
                    if (p[i] != 0) {
                        theUnsafe.freeMemory(p[i]);
                        p[i] = 0;
                        if ((i % 10000) == 0) {
                            sleep_delay(free_delay);
                        }
                    }
                }
            }
            freed += numToFree;
        }

        void freeAll() {
            freeSome(p.length);
        }

        void freeSome(int numToFree) {
            if (freeType == FreeType.interleaved) {
                freeInterleaved(numToFree);
            } else {
                freeInOrder(numToFree);
            }
        }
    }

    static boolean stopnoise = false;
    static Unsafe theUnsafe;
    static Random random = new Random();

    class NoiseThread extends Thread {
        Allocator a = new Allocator(10, 5555);
        @Override
        public void run() {
            while(!stopnoise) {
                a.allocateAll();
                a.freeAll();
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

    int randomized_allocation_size() {
        if (randfactor != 1) {
            int lower_bound = allocationSize;
            int upper_bound = allocationSize * randfactor;
            int randsize = random.nextInt(upper_bound - lower_bound) + lower_bound;
            return randsize;
        }
        return allocationSize;
    }

    void seed_the_randomizer() {
        if (randseed == 0) {
            randseed = random.nextLong();
        }
        random.setSeed(randseed);
    }

    @Override
    public Integer call() throws Exception {

        initialize(false, auto_yes, nowait);

        seed_the_randomizer();

        System.out.println("999999");

        System.out.println("Number of Cylces: " + numCycles);
        System.out.println("Number of Allocations: " + numAllocations);
        System.out.println("Allocation Size: " + allocationSize);
        System.out.println("Size Random factor: " + randfactor);
        System.out.println("Touch: " + !notouch);
        System.out.println("Type: " + testType);
        System.out.println("Noise: " + noise);
        System.out.println("Randomizer seed: " + randseed);
        System.out.println("Alloc delay: " + alloc_delay);
        System.out.println("Free delay: " + free_delay);

        waitForKeyPress("Lets start?", 6);

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        theUnsafe = (Unsafe) f.get(null);

        Thread[] noiseThreads = new Thread[noise];

        if (noise > 0) {
            for (int i = 0; i < noise; i ++) {
                noiseThreads[i] = new NoiseThread();
                noiseThreads[i].start();
            }
        }

        Allocator a = new Allocator(numAllocations, randseed);

        for (int cycle = 0; cycle < numCycles; cycle ++) {

            waitForKeyPress("Cycle " + cycle + ": before allocation...", waitsecs);

            a.allocateAll();

            waitForKeyPress("Cycle " + cycle + ": allocation phase completed.", 0);

            if (testType == TestType.peak) {
                waitForKeyPress("Cycle " + cycle + ": before free...", 0);
                a.freeAll();
                waitForKeyPress("Cycle " + cycle + ": free phase completed.", 0);
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
