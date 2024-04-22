package de.stuefe.repros;

import picocli.CommandLine;

import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "AdvantestRepro", mixinStandardHelpOptions = true,
        description = "AdvantestRepro.")
public class AdvantestRepro extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--auto-yes", "-y" },
            description = "Autoyes (default: ${DEFAULT-VALUE}).")
    boolean auto_yes = false;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes) (default: ${DEFAULT-VALUE}).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--waitsecs" },
            description = "wait n seconds (only with autoyes) (default: ${DEFAULT-VALUE}).")
    int unattendedWaitSecs = 4;

    @CommandLine.Option(names = { "--cycles" },
            description = "Number of repeats (default: ${DEFAULT-VALUE})")
    int numCycles = 10;

    @CommandLine.Option(names = { "--initial-liveset-mb" },
            description = "Size of initial liveset (default: ${DEFAULT-VALUE})")
    int initial_liveset_mb = 0;

    public static void main(String... args) {
        int exitCode = new CommandLine(new AdvantestRepro()).execute(args);
        System.exit(exitCode);
    }

    volatile Object[] retain;
    volatile byte[][] retain2;

    @Override
    public Integer call() throws Exception {

        initialize(false, auto_yes, nowait, unattendedWaitSecs);

        System.out.println("Number of Cylces: " + numCycles);

        System.out.println("Building initial set...");
        if (initial_liveset_mb > 0) {
            int num_old_blocks = initial_liveset_mb * 1024;
            int headersize = 16;
            retain2 = new byte[num_old_blocks][];
            for (int i = 0; i < num_old_blocks; i ++) {
                retain2[i] = new byte[1024 - headersize];
            }
        }

        waitForKeyPress("System.gc...");
        Thread.sleep(4000);
        System.gc();
        System.gc();

        for (int cycle = 0; cycle < numCycles; cycle ++) {

            waitForKeyPress("allocing...", 4);

            retain = new Object[1024*1024];

            for (int i = 0; i < 1024*1024; i++ ) {
                retain[i] = new Object();
            }

            waitForKeyPress("nulling...");

            retain = null;
        }

        waitForKeyPress("Done.");

        return 0;
    }

}
