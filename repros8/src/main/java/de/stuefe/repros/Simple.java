package de.stuefe.repros;

import picocli.CommandLine;

import java.util.concurrent.Callable;

public class Simple extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--stdout", "-1" },
            description = "Print given text to stdout (default: ${DEFAULT-VALUE}).")
    String stdoutText = "Hallo Stdout";

    @CommandLine.Option(names = { "--stderr", "-2" },
            description = "Print given text to stderr (default: ${DEFAULT-VALUE}).")
    String stderrText = "Hallo Stderr";

    @CommandLine.Option(names = { "--return", "-r" },
            description = "Return value (default: ${DEFAULT-VALUE}).")
    int rc = 0;

    @CommandLine.Option(names = { "--time", "-t" },
            description = "Print times (default: ${DEFAULT-VALUE}).")
    boolean printTimes = false;

    @CommandLine.Option(names = { "--autoyes", "-y" },
            description = "Autoyes (default: ${DEFAULT-VALUE}).")
    boolean auto_yes = false;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes) (default: ${DEFAULT-VALUE}).")
    boolean nowait = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new Simple()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        initialize(false, auto_yes, nowait);
        System.out.println(stdoutText);
        System.err.println(stderrText);
        waitForKeyPress();
        return rc;
    }

}
