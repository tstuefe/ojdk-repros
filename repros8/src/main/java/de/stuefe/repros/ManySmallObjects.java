package de.stuefe.repros;

// Really simple stupid test
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "ManySmallObjects", mixinStandardHelpOptions = true,
     description = "ManySmallObjects repro.")
public class ManySmallObjects extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num", "-n" },
            description = "Number of objects (default: ${DEFAULT-VALUE}).")
    int num_objects = 1024 * 1024 * 256;

    @CommandLine.Option(names = { "--size", "-s" },
            description = "Size of objects (-1 means Object, 0 means zero-leght byte array etc) (default: ${DEFAULT-VALUE}).")
    int size_objects = -1;

    @CommandLine.Option(names = { "--autoyes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    @CommandLine.Option(names = { "--gc" },
            description = "do a gc at the end (default: ${DEFAULT-VALUE})@")
    boolean gc;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ManySmallObjects()).execute(args);
        System.exit(exitCode);
    }

    static class D {
        int i;
    }

    public volatile Object[] o;

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Will create " + num_objects + " nearly empty objects...");

        o = new Object[num_objects];

        for (int i = 0; i < num_objects; i ++) {
            o[i] = size_objects == -1 ? new Object() : new byte[size_objects];
        }

        if (gc) {
            waitForKeyPress("Before gc.");
            System.gc();
        }

        waitForKeyPress("Done.");

        return 0;
    }

}
