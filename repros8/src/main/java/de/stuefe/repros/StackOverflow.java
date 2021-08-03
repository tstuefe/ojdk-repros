package de.stuefe.repros;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "Hello", mixinStandardHelpOptions = true,
        description = "Just a simple Hello World.")
public class StackOverflow extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--wait" },
            description = "wait for key press (default: ${DEFAULT-VALUE})")
    boolean do_wait = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new StackOverflow()).execute(args);
        System.exit(exitCode);
    }

    private int foo(int i) {
        if (i > 0) {
            i += foo(i + 1);
        }
        i /= 2;
        return i;
    }

    @Override
    public Integer call() throws Exception {
        if (do_wait) {
            waitForKeyPress();
        }
        int j = foo(2);
        System.out.println("_" + j);
        return j;
    }

}
