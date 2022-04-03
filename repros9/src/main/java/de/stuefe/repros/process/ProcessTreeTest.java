package de.stuefe.repros.process;

public class ProcessTreeTest {

    static void printTree(ProcessHandle ph, int depth) {
        for (int i = 0; i < depth; i ++) {
            System.out.print("---");
        }
        System.out.println(" " + ph.pid() + "  " + ph.info().toString());
        ph.children().forEach((p) -> printTree(p, depth + 1));
    }


    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println("Usage: ProcessTreeTest <program path>");
            return;
        }

        String[] cmd = new String[args.length];
        for (int i = 0; i < args.length; i ++) {
            cmd[i] = args[i];
        }

        Process p = Runtime.getRuntime().exec(cmd);
        System.out.println("Process started.");
        ProcessHandle ph = p.toHandle();

        char c = '\0';
        while (c != 'q') {
            printTree(ph, 0);
            System.out.println("Press q to quit, any other key to reprint the tree");
            c = (char)System.in.read();
        }
    }

}
