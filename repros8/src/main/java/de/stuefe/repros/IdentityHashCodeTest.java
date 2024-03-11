package de.stuefe.repros;

// Really simple stupid test
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "IdentityHashCodeTest", mixinStandardHelpOptions = true,
     description = "IdentityHashCodeTest repro.")
public class IdentityHashCodeTest extends TestCaseBase implements Callable<Integer> {

    class TestObject {
        public Object next;
    };

    @CommandLine.Option(names = { "--num", "-n" },
            description = "Number of objects (default: ${DEFAULT-VALUE}).")
    int num_objects = 1024 * 1024 * 256;

    @CommandLine.Option(names = { "--autoyes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    public static void main(String... args) {
        int exitCode = new CommandLine(new IdentityHashCodeTest()).execute(args);
        System.exit(exitCode);
    }

    static class D {
        int i;
    }

    public volatile D[] oa;

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Will create " + num_objects + " nearly empty objects...");

        oa = new D[num_objects];
        int[] distribution_map = new int [0x800];

        int hi = 0, lo = 0;
        for (int i = 0; i < num_objects; i ++) {
            D o = new D();
            int h = System.identityHashCode(o);
            if (h > hi) hi = h;
            if (h < lo) lo = h;
            o.i = h;
            oa[i] = o;
            int distribution_map_idx = h / (1024 * 1024);
            distribution_map[distribution_map_idx] ++;
        }

        System.out.println("highest/lowest ihash: " + lo + " .. " + hi);
        System.out.println("Distribution:");
        for (int i = 0; i < distribution_map.length; i++) {
            System.out.print(distribution_map[i] + "-");
        }

        waitForKeyPress("Before gc.");

        // kill 3/4 of all objects; do a gc and hope the rest of the objects get moved.
        for (int i = 0; i < num_objects; i++) {
            if ((i % 4) > 0) {
                oa[i] = null;
            }
        }

        System.gc();
        System.gc();
        System.gc();

        waitForKeyPress("Test hash codes.");

        for (int i = 0; i < distribution_map.length; i++) {
            if (oa[i] != null) {
                if (oa[i].i != oa[i].hashCode()) {
                    throw new RuntimeException("???");
                }
            }
        }

        waitForKeyPress("Done.");

        return 0;
    }

}
