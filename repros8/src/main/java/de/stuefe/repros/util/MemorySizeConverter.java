package de.stuefe.repros.util;

import picocli.CommandLine;

public final class MemorySizeConverter implements CommandLine.ITypeConverter<Long> {
    public MemorySizeConverter() {}
    /**
     * Converts the specified command line argument value to some domain object.
     * @param value the command line argument String value
     * @return the resulting domain object
     * @throws Exception an exception detailing what went wrong during the conversion
     */
    public Long convert(String value) throws Exception {
        return new Long(MemorySize.parseMemorySize(value));
    }
}

