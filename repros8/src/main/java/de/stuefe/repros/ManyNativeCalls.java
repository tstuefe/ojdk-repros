package de.stuefe.repros;

// Really simple stupid test
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "ManySmallObjects", mixinStandardHelpOptions = true,
     description = "ManySmallObjects repro.")
public class ManyNativeCalls extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num", "-n" },
            description = "Number of objects (default: ${DEFAULT-VALUE}).")
    int num = 1024 * 1024 * 256;

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
        int exitCode = new CommandLine(new ManyNativeCalls()).execute(args);
        System.exit(exitCode);
    }

    static class D {
        int i;
    }

    public volatile Object[] o;

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Will call  " + num + " times to native...");

        for (int i = 0; i < num; i ++) {
            if (0 == (i % 1000)) {
                System.out.println("*");
            }
            try  {
                FileInputStream fs = new FileInputStream("hallo");
                fs.close();
            } catch (IOException e) {}
        }

        waitForKeyPress("Done.");

        return 0;
    }

}
