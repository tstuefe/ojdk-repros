package de.stuefe.repros.util;

public enum MemorySize {
    K (1024),
    M (1024 * 1024),
    G (1024 * 1024 * 1024);

    private long val;

    public final long value() { return val; }

    MemorySize(long i) {
        val = i;
    }

    public static final boolean isMemorySize(String s) {
        return s.endsWith(K.name()) ||
                s.endsWith(M.name()) ||
                s.endsWith(G.name());
    }

    public static final long parseMemorySize(String value) throws NumberFormatException {
        String s = value.trim().toUpperCase();
        long l = -1;
        try {
            for (MemorySize ms : values()) {
                if (s.endsWith(ms.name())) {
                    return Long.parseLong(s.substring(0, s.length() - 1)) * ms.value();
                }
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + value + "\" is not a valid memory size");
        }
    }

}
