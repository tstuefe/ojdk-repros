package de.stuefe.repros;

import picocli.CommandLine;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "Skeleton", mixinStandardHelpOptions = true,
        description = "Skeleton setup")
public class Skeleton extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--auto-yes", "-y" },
            description = "Autoyes.")
    boolean auto_yes = false;

    @CommandLine.Option(names = { "--waitsecs" },
            description = "If autoyes, how many seconds to wait per step (default: ${DEFAULT-VALUE}).")
    int waitsecs = 0;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    public static void main(String... args) {
        int exitCode = new CommandLine(new Skeleton()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        initialize(verbose, auto_yes, nowait);

        System.out.println("JDK11 version");
        
        return 0;
    }

}
