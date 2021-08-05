package de.stuefe.repros;


import picocli.CommandLine;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "DBBTest", mixinStandardHelpOptions = true,
        description = "Simple direct bytebuffer test.")
public class DBBTest extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num-buffers" },
            description = "Number of buffers (default: ${DEFAULT-VALUE})")
    int num_buffers = 4;

    @CommandLine.Option(names = { "--size-buffers" },
            description = "Size, in MB, of buffers (default: ${DEFAULT-VALUE})")
    int size_buffers = 4;

    public static void main(String... args) {
        int exitCode = new CommandLine(new DBBTest()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        System.out.println("<press key>");
        System.in.read();
        System.out.println("Allocating " + num_buffers + " buffers a " + size_buffers + " bytes...");
        ByteBuffer buffers[] = new ByteBuffer[num_buffers];
        for (int i = 0; i < num_buffers; i ++) {
            ByteBuffer b = ByteBuffer.allocateDirect(size_buffers);
            buffers[i] = b;
        }

        System.out.println("<press key for GC>");
        System.in.read();

        for (int i = 0; i < num_buffers; i ++) {
            buffers[i] = null;
        }
        System.gc();
        System.out.println("<press key>");
        System.in.read();
        return 0;
    }

}
