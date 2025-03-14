package de.stuefe.repros;

// Really simple stupid test
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "ManySmallObjects", mixinStandardHelpOptions = true,
     description = "ManySmallObjects repro.")
public class ManySmallObjects extends TestCaseBase implements Callable<Integer> {

    class TestObject {
        public Object next;
    };

    @CommandLine.Option(names = { "--num", "-n" },
            description = "Number of objects (default: ${DEFAULT-VALUE}).")
    int num_objects = 1024 * 1024 * 256;

    @CommandLine.Option(names = { "--size", "-s" },
            description = "Size of objects (-2 means test object with one embedded oop, -1 means Object, 0 means zero-leght byte array etc) (default: ${DEFAULT-VALUE}).")
    int size_objects = -1;

    @CommandLine.Option(names = { "--autoyes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    @CommandLine.Option(names = { "--print-ihash" }, defaultValue = "false",
            description = "print ihash.")
    boolean print_ihash;

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

    public volatile Object[] oa;

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Will create " + num_objects + " nearly empty objects...");

        oa = new Object[num_objects];

        for (int i = 0; i < num_objects; i ++) {
            Object o;
            TestObject last_o = null;
            switch (size_objects) {
                case -2:
                    o = new TestObject();
                    if (last_o != null) last_o.next = o;
                    last_o = (TestObject) o;
                    break;
                case -1:
                    o = new Object();
                    break;
                default:
                    o = new byte[size_objects];
            }
            oa[i] = o;
        }

        if (print_ihash) {
            for (int i = 0; i < oa.length; i++) {
                System.out.println(System.identityHashCode(oa[i]));
            }
        }

        waitForKeyPress("Befire release.");

        oa = null;

        if (gc) {
            waitForKeyPress("Before gc.");
            System.gc();
        }

        waitForKeyPress("Done.");

        return 0;
    }

}
