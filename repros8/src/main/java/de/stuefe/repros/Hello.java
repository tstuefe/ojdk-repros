package de.stuefe.repros;

import picocli.CommandLine;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "Hello", mixinStandardHelpOptions = true,
        description = "Just a simple Hello World.")
public class Hello extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--wait" },
            description = "wait for key press (default: ${DEFAULT-VALUE})")
    boolean do_wait = false;

    @CommandLine.Option(names = { "--returnval" },
            description = "program return value (default: ${DEFAULT-VALUE})")
    int returnval = 0;

    public static void main(String... args) {
        int exitCode = new CommandLine(new Hello()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Hello (stdout)");
        System.err.println("Hello (stderr)");
        if (do_wait) {
            waitForKeyPress();
        }
        System.out.println("Bye... (stdout)");
        System.err.println("Bye... (stderr)");
        return returnval;
    }

}
