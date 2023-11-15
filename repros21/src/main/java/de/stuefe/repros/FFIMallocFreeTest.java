package de.stuefe.repros;

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

        initialize(verbose, auto_yes, nowait);

        try {
            for (int i = 0; i < 1000; i++) {
                invokeMalloc(1024 * 3);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

}
