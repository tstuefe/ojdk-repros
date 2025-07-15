package de.stuefe.repros;

// Really simple stupid test
import picocli.CommandLine;

import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "ManyNonASCIIStrings", mixinStandardHelpOptions = true,
     description = "ManyNonASCIIStrings repro.")
public class ManyNonASCIIStrings extends TestCaseBase implements Callable<Integer> {


    @CommandLine.Option(names = { "--num", "-n" },
            description = "Number of NonAscii strings (default: ${DEFAULT-VALUE}).")
    int num = 1024 * 1024;

    @CommandLine.Option(names = { "--len", "-l" },
            description = "Length of NonAscii strings (default: ${DEFAULT-VALUE}).")
    int len = 0x20;

    @CommandLine.Option(names = { "--gc" },
            description = "do a gc at the end (default: ${DEFAULT-VALUE})")
    boolean gc = false;

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
        int exitCode = new CommandLine(new ManyNonASCIIStrings()).execute(args);
        System.exit(exitCode);
    }

    public String[] strings;

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Will create " + num + " nonascii strings of len " + len + "...");

        // Build randomized strings with hiragana
        int codepoint_start = 0x3041, codepoint_end = 0x3094;

        strings = new String[num];
        java.util.Random r = new Random();

        for (int i = 0; i < num; i ++) {
            StringBuilder bld = new StringBuilder();
            for (int j = 0; j < len; j ++) {
                int codepoint = r.nextInt(codepoint_start, codepoint_end);
                bld.append(new String(Character.toChars(codepoint)));
            }
            strings[i] = bld.toString();
            if (i < 10) {
                System.out.println(strings[i]);
            } else if (i == 10) {
                System.out.println("...");
            }
        }

        waitForKeyPress("Befire release.");

        strings = null;

        if (gc) {
            waitForKeyPress("Before gc.");
            System.gc();
        }

        waitForKeyPress("Done.");

        return 0;
    }

}
