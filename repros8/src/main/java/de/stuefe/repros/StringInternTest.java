package de.stuefe.repros;

import picocli.CommandLine;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "StringInternTest", mixinStandardHelpOptions = true,
        description = "StringInternTest.")
public class StringInternTest extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--auto-yes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    @CommandLine.Option(names = { "--num" },
            description = "Number of strings (default: ${DEFAULT-VALUE})")
    int num = 1024*1024*64;

    @CommandLine.Option(names = { "--cycles" },
            description = "Number of repeats (default: ${DEFAULT-VALUE})")
    int numCycles = 3;

    public static void main(String... args) {
        int exitCode = new CommandLine(new StringInternTest()).execute(args);
        System.exit(exitCode);
    }

    static boolean stopnoise = false;
    static Random random = new Random();

    static String generateRandomString() {
        StringBuilder bld = new StringBuilder(128);
        int len = random.nextInt(126) + 2;
        for (int i = 0; i < len; i ++) {
            bld.append('A' + random.nextInt(26));
        }
        return bld.toString();
    }

    public volatile String[] strings;

    @Override
    public Integer call() throws Exception {

        initialize(false, auto_yes, nowait);

        System.out.println("Number of Cylces: " + numCycles);
        System.out.println("Number of Allocations: " + num);

        for (int cycle = 0; cycle < numCycles; cycle ++) {

            waitForKeyPress("Preparing strings...");

            strings = new String[num];
            for (int i = 0; i < num; i ++) {
                strings[i] = generateRandomString();
            }

            waitForKeyPress("Cycle " + cycle + ": before interning...");

            for (int i = 0; i < num; i ++) {
                strings[i] = strings[i].intern();
            }
/*
            waitForKeyPress("Cycle " + cycle + ": interning phase completed, before gc.");

            strings = null;
            System.gc();
            System.gc();

            waitForKeyPress("Cycle " + cycle + ": gc completed.");
*/
            int matches_with_0 = 0;
            for (int i = 0; i < num; i ++) {
                if (strings[i] == strings[0]) {
                    matches_with_0 ++;
                }
            }
            System.out.println("matches " + matches_with_0);

        }

        waitForKeyPress("Done.");

        return 0;
    }

}
