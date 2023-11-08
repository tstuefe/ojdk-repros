package de.stuefe.repros;

import picocli.CommandLine;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.Callable;

public class OPENJDK_2352  {

    public static volatile Date d;

    static void waitForKeyPress() {
        try {
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String... args) {
        boolean doit = false;
        if (args.length > 0) {
            doit = Boolean.parseBoolean(args[0]);
        }
        System.out.println("started ");
        waitForKeyPress();
        LocalDateTime d = null;
        if (doit) {
            d = LocalDateTime.now();
        }
        System.gc();
        System.out.println("Now:" + ((d != null) ? d.toString() : "none"));
        waitForKeyPress();
    }

}
