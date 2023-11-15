package de.stuefe.repros;

import picocli.CommandLine;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "FFI1Test", mixinStandardHelpOptions = true,
        description = "FFI1Test")
public class FFI1Test extends TestCaseBase implements Callable<Integer> {

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
        int exitCode = new CommandLine(new FFI1Test()).execute(args);
        System.exit(exitCode);
    }

    static long invokeStrlen(String s) throws Throwable {

        try (Arena arena = Arena.ofConfined()) {

            // Allocate off-heap memory and
            // copy the argument, a Java string, into off-heap memory
            MemorySegment nativeString = arena.allocateUtf8String(s);

            // Link and call the C function strlen

            // Obtain an instance of the native linker
            Linker linker = Linker.nativeLinker();

            // Locate the address of the C function signature
            SymbolLookup stdLib = linker.defaultLookup();
            MemorySegment strlen_addr = stdLib.find("strlen").get();

            // Create a description of the C function
            FunctionDescriptor strlen_sig =
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);

            // Create a downcall handle for the C function
            MethodHandle strlen = linker.downcallHandle(strlen_addr, strlen_sig);

            // Call the C function directly from Java
            return (long)strlen.invokeExact(nativeString);
        }
    }

    @Override
    public Integer call() throws Exception {

        initialize(verbose, auto_yes, nowait);

        try {
            System.out.println("strlen hallo " + invokeStrlen("hallo"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

}
