package de.stuefe.repros.process;

import de.stuefe.repros.TestCaseBase;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "RunProcess", mixinStandardHelpOptions = true,
        description = "RunProcess repro.")
public class RunProcess extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--repeat", "-n" }, description = "Autoyes.")
    int repeat = 1;

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

    @CommandLine.Parameters(index = "0..*", arity = "1")
    List<String> program_and_args;

    public static void main(String... args) {
        int exitCode = new CommandLine(new RunProcess()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        if (program_and_args.size() < 1) {
            System.out.println("Usage: RunProcess <program> <args>");
            return -1;
        }

        String[] cmd = new String[program_and_args.size()];
        program_and_args.toArray(cmd);

        waitForKeyPress("Will start " +
                        String.join(" ", program_and_args) + " nearly empty objects...");

        for (int i = 0; i < repeat; i++) {
            try {
                System.out.println("Run: " + i + "...");
                ProcessBuilder bld = new ProcessBuilder().command(cmd).inheritIO();
                Process p = bld.start();
                p.waitFor();
                System.out.println("Process finished (exitcode " + p.exitValue() + ")");
            } catch (IOException e) {
                System.out.println("Error: " + e.toString());
                e.printStackTrace();
            }
        }
        return 0;
    }


}
