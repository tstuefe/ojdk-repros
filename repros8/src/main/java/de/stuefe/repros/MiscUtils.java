package de.stuefe.repros;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

public class MiscUtils {

    private static final boolean unattended;

    static {
        String propertyName = "de.stuefe.unattended";
        unattended = AccessController.doPrivileged((PrivilegedAction<Boolean>)
                ()->Boolean.getBoolean(propertyName));
    }

    public static void waitForKeyPress(String message) {
        System.out.println(message);
        waitForKeyPress();
    }

    public static void waitForKeyPress() {
        System.out.println("<press key>");
        if (unattended) {
            System.out.println("(suppressed)");
            return;
        }
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
