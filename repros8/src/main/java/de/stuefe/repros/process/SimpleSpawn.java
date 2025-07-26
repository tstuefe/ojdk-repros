package de.stuefe.repros.process;

import de.stuefe.repros.TestCaseBase;
import picocli.CommandLine;

import java.util.List;
import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "SimpleSpawn", mixinStandardHelpOptions = true,
        description = "SimpleSpawn repro.")
public class SimpleSpawn extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num", "-n" },
            description = "Number of child processes (default: ${DEFAULT-VALUE})")
    int num = 1;

    @CommandLine.Option(names = { "--ignore-errors", "-i" }, description = "quietly swallow and ignore IOExceptions (default: ${DEFAULT-VALUE}")
    boolean ignore_errors = false;


    @CommandLine.Option(names = { "--workdir", "-d" }, description = "current working directory (default: ${DEFAULT-VALUE}")
    String workdir;

    @CommandLine.Option(names = { "--autoyes", "-y" },
            description = "Autoyes.")
    boolean auto_yes = false;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" }, description = "do not wait (only with autoyes).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" }, description = "Verbose.")
    boolean verbose = false;

    @CommandLine.Parameters(index = "0..*", description = "Command line(s) to run", arity = "1..*")
    List<String> command;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new SimpleSpawn()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command).inheritIO();

        if (workdir != null) {
            builder = builder.directory(new File(workdir));
        }

        waitForKeyPress("Before spawning child(s)...");
        Process[] processes = new Process[num];

        for (int i = 0; i < num; i ++) {
            try {
                processes[i] = builder.start();
            } catch (Exception e) {
                processes[i] = null;
                System.err.println("Process " + i + " failed to start");
                System.err.println(e.getMessage());
                if (!ignore_errors) {
                    System.exit(-1);
                } else {
                    waitForKeyPress("Chils spwan ero...");
                }
            }
            if (verbose) {
                System.out.println("Process " + i + " started.");
            }
        }

        for (int i = 0; i < num; i ++) {
            if (processes[i] == null) {
                continue;
            }
            processes[i].waitFor();
            if (verbose) {
                System.out.println("Process " + i + " finished (exitcode " + processes[i].exitValue() + ")");
            }
        }

        System.out.println("All process(es) finished.");

        waitForKeyPress("Before exit...");

        return 0;
    }

}
