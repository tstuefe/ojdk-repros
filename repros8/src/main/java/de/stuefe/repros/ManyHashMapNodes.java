package de.stuefe.repros;

// Really simple stupid test
import picocli.CommandLine;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "ManyHashMapNodes", mixinStandardHelpOptions = true,
     description = "ManyNonASCIIStrings repro.")
public class ManyHashMapNodes extends TestCaseBase implements Callable<Integer> {


    @CommandLine.Option(names = { "--num", "-n" },
            description = "Number of nodes (default: ${DEFAULT-VALUE}).")
    int num = 1024 * 1024;


    @CommandLine.Option(names = { "--cycles", "-c" }, description = "Number of GC cycles (default: ${DEFAULT_VALUE})")
    int cycles = 10;

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
        int exitCode = new CommandLine(new ManyHashMapNodes()).execute(args);
        System.exit(exitCode);
    }


    public HashMap<String, Object> _map;
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Will create " + num + " nodes ...");

        _map = new HashMap();

        for (int i = 0; i < num; i ++) {
            String k = Integer.toString(i);
            Object o;
            switch(i % 16) {
                case 0:
                case 1: o = Long.valueOf(i); break;
                case 2:
                case 3: o = Integer.valueOf(i); break;
                case 4:
                case 5: o = new Object(); break;
                case 6:
                case 7: o = new Object[1]; break;
                case 8:
                case 9: o = new String[1]; break;
                case 10:
                case 11: o = new byte[1]; break;
                case 12:
                case 13: o = new int[1]; break;
                default:
                    o = Integer.toString(i, 10);
            }
            _map.put(k, o);
        }

        System.out.println(_map.get("ABC"));

        waitForKeyPress("Before GCs ...");

        for (int i = 0; i < cycles; i++) {
            System.gc();
        }

        waitForKeyPress("Before release.");

        _map = null;

        System.gc();

        waitForKeyPress("Done.");

        return 0;
    }

}
