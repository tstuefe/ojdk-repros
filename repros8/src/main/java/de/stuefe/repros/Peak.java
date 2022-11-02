package de.stuefe.repros;

import de.stuefe.repros.util.MemorySize;
import de.stuefe.repros.util.MemorySizeConverter;
import picocli.CommandLine;

import java.util.concurrent.Callable;


@CommandLine.Command(name = "Peak", mixinStandardHelpOptions = true,
        description = "Just burn cpu on a number of threads.")
public class Peak extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "-s", "--size" },
            description = "Size (default: ${DEFAULT-VALUE})")
    int size = 1024 * 256;

    @CommandLine.Option(names = { "--num", "-n" },
            description = "Number of allocations..")
    int numAllocations = 1024;

    @CommandLine.Option(names = { "--auto-yes", "-y" },
            description = "Autoyes.")
    boolean auto_yes = false;

    @CommandLine.Option(names = { "--cycles", "-c" },
            description = "Number of repeats (default: ${DEFAULT-VALUE})")
    int numCycles = 5;

    @CommandLine.Option(names = { "--explicit-gc", "-g" },
            description = "Do explicit gc? (default: ${DEFAULT-VALUE})\")")
    boolean explicit_gc = false;

    @CommandLine.Option(names = { "--waitsecs" },
            description = "If autoyes, how many seconds to wait per step (default: ${DEFAULT-VALUE}).")
    int waitsecs = 4;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" },
            description = "verbose mode.")
    boolean verbose = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new Peak()).execute(args);
        System.exit(exitCode);
    }

    byte[][] arr;

    @Override
    public Integer call() throws Exception {

        initialize(verbose, auto_yes, nowait);

        System.out.println("Size: " + size);
        System.out.println("Num: " + numAllocations);
        System.out.println("Cycles: " + numCycles);
        System.out.println("GC?: " + explicit_gc);

        System.out.println("Estimated heap usage (64-bit VM): " +
                ((long)numAllocations * 8 + // base array
                 (long)numAllocations * (16 + size)) // secondary arrays, including headers
                        / (long)(1024 * 1024) + "MB.");

        for (int cycle = 0; cycle < numCycles; cycle++) {
            waitForKeyPress("Cycle " + cycle + ": before allocation...", waitsecs);
            try {
                arr = new byte[numAllocations][];
                for (int n = 0; n < numAllocations; n++) {
                    arr[n] = new byte[size];
                }
                waitForKeyPress("Cycle " + cycle + ": after allocation, before release...", waitsecs);
            } finally {
                arr = null;
            }
            if (explicit_gc) {
                waitForKeyPress("Cycle " + cycle + ": after release, before GC...", waitsecs);
                System.gc();
            }
        }

        waitForKeyPress("Finished.", waitsecs);

        return  0;
    }


}
