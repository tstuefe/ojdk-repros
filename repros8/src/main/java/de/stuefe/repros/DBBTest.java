package de.stuefe.repros;

import de.stuefe.repros.util.MemorySize;
import de.stuefe.repros.util.MemorySizeConverter;
import picocli.CommandLine;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "DBBTest", mixinStandardHelpOptions = true,
        description = "Simple direct bytebuffer test.")
public class DBBTest extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--num-buffers", "-n" },
            description = "Number of buffers (default: ${DEFAULT-VALUE})")
    int numBuffers = 64;

    @CommandLine.Option(names = { "--size-buffers", "-s" }, converter = MemorySizeConverter.class,
            description = "Size of buffers (default: ${DEFAULT-VALUE})")
    Long bufferSizeL = MemorySize.M.value();
    int bufferSize;

    enum TestType { peak, leak, partleak };
    @CommandLine.Option(names = { "--type", "-t" },
            description = "Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    TestType testType = TestType.peak;

    @CommandLine.Option(names = { "--touch" }, negatable = true,
            description = "Touch allocated memory (default: true).")
    Boolean touchB = null;
    boolean touch = false;

    @CommandLine.Option(names = { "--cycles", "-c" },
            description = "Number of repeats (default: ${DEFAULT-VALUE})")
    int numCycles = 5;

    @CommandLine.Option(names = { "--auto-yes", "-y" },
            description = "Autoyes.")
    boolean auto_yes = false;

    @CommandLine.Option(names = { "--waitsecs" },
            description = "If autoyes, how many seconds to wait per step (default: ${DEFAULT-VALUE}).")
    int waitsecs = 4;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" },
            description = "verbose mode.")
    boolean verbose = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new DBBTest()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        initialize(verbose, auto_yes, nowait);

        bufferSize = this.bufferSizeL.intValue();
        touch = (touchB != null) ? touchB.booleanValue() : true;

        System.out.println("Type: " + testType);
        System.out.println("Num buffers: " + numBuffers);
        System.out.println("buffer size: " + bufferSize);
        System.out.println("Touch: " + touch);
        System.out.println("Cycles: " + numCycles);

        List<ByteBuffer> l = new LinkedList<ByteBuffer>();

        for (int nCycle = 0; nCycle < numCycles; nCycle ++) {
            waitForKeyPress("Cycle " + nCycle + ": before allocation...", waitsecs);

            for (int i = 0; i < numBuffers; i ++) {

                ByteBuffer b = ByteBuffer.allocateDirect(bufferSize);
                l.add(b);
                if (touch) {
                    int stride = (int) (MemorySize.K.value() * 4);
                    while (b.position() < b.capacity()) {
                        b.put((byte) 'A');
                        if (b.position() < (b.capacity() - stride)) {
                            b.position(b.position() + stride);
                        }
                    }
                }
            }

            switch (testType) {
                case peak:
                    waitForKeyPress("Cycle " + nCycle + ": after allocation, before release + GC...", waitsecs);
                    l.clear();
                    System.gc();
                    System.gc();
                    waitForKeyPress("Cycle " + nCycle + ": after GC...", waitsecs);
                    break;
                case partleak:
                    waitForKeyPress("Cycle " + nCycle + ": after allocation, before part release + GC...", waitsecs);
                    for (int i = 0; i < numBuffers / 2; i ++) {
                        l.remove(0);
                    }
                    System.gc();
                    System.gc();
                    waitForKeyPress("Cycle " + nCycle + ": after GC...", waitsecs);
                    break;
                case leak:
                    waitForKeyPress("Cycle " + nCycle + ": after allocation, before GC...", waitsecs);
                    System.gc();
                    System.gc();
                    waitForKeyPress("Cycle " + nCycle + ": after GC...", waitsecs);
                    break;
            }
        }

        return 0;
    }

}
