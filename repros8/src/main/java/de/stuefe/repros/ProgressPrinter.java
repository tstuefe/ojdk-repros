package de.stuefe.repros;

class ProgressPrinter {
    int i = 0;
    long t1 = 0;
    final int cycle;

    public ProgressPrinter(int cycle) {
        this.cycle = cycle;
        this.t1 = System.currentTimeMillis();
    }

    public ProgressPrinter() {
        this(100);
    }

    public void done() {
        long t2 = System.currentTimeMillis();
        System.out.println("Done (" + (t2 - t1) + "ms).");
    }

    public void reset() {
        i = 0;
    }

    public void inc() {
        i ++;
        if ((i % cycle) == 0) {
            System.out.print('.');
        }
    }

}
