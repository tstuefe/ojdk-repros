package de.stuefe.repros.process;

import de.stuefe.repros.TestCaseBase;
import picocli.CommandLine;

import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "PrintProcess", mixinStandardHelpOptions = true,
        description = "PrintProcess setup")
public class PrintProcess extends TestCaseBase implements Callable<Integer> {

    boolean auto_yes = false;
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    @CommandLine.Parameters(index = "0..*", arity = "1..*", description = "Process Identifiers")
    int pids[];

    public static void main(String... args) {
        int exitCode = new CommandLine(new PrintProcess()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        initialize(verbose, auto_yes, nowait);

        for (int pid: pids) {
            Optional<ProcessHandle> pho = ProcessHandle.of(pid);
            if (pho.isPresent()) {
                ProcessHandle ph = pho.get();
                System.out.println(ph.pid() + ": alive: " + ph.isAlive() + ", " + ph.info());
            } else {
                System.out.println("not found: " + pid);
            }
        }

        return 0;
    }

}
