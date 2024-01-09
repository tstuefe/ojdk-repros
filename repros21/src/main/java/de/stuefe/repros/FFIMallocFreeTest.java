package de.stuefe.repros;

import de.stuefe.repros.util.MemorySizeConverter;
import picocli.CommandLine;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "FFIMallocFreeTest", mixinStandardHelpOptions = true,
        description = "FFIMallocFreeTest")
public class FFIMallocFreeTest extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--auto-yes", "-y" },
            description = "Autoyes.")
    boolean auto_yes = false;

    @CommandLine.Option(names = { "--waitsecs" },
            description = "If autoyes, how many seconds to wait per step (default: ${DEFAULT-VALUE}).")
    int waitsecs = 0;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    @CommandLine.Option(names = { "--num", "-n" }, converter = MemorySizeConverter.class,
            description = "Number of allocations. Accepts g, m, k suffixes. Default: ${DEFAULT-VALUE}.")
    Long numAllocationsL = null;
    int numAllocations = -1;

    @CommandLine.Option(names = { "--size", "-s" }, converter = MemorySizeConverter.class,
            description = "Allocation size. Accepts g, m, k suffixes. Default: ${DEFAULT-VALUE}.")
    Long allocationSizeL = null;
    long allocationSize = -1;

    public static void main(String... args) {
        int exitCode = new CommandLine(new FFIMallocFreeTest()).execute(args);
        System.exit(exitCode);
    }

    static long invokeMalloc(long byteSize) throws Throwable {

        Linker linker = Linker.nativeLinker();
        var malloc_addr = linker.defaultLookup().find("malloc").orElseThrow();
        MethodHandle malloc = linker.downcallHandle(
                malloc_addr,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        MemorySegment segment = (MemorySegment) malloc.invokeExact(byteSize);
        System.out.println(segment);
        return segment.address();

    }

    @Override
    public Integer call() throws Exception {

        allocationSize = allocationSizeL == null ? 1024 : allocationSizeL.longValue();
        numAllocations = numAllocationsL == null ? 1024 : numAllocationsL.intValue();
        System.out.println("num: " + numAllocations + ", size " + allocationSize + ", expected net footprint " + (allocationSize * numAllocations));
        initialize(verbose, auto_yes, nowait);

        try {
            for (int i = 0; i < numAllocations; i++) {
                invokeMalloc(allocationSize);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

}
