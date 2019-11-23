package de.stuefe.repros;

import java.io.IOException;

public class TestCaseBase {

    private boolean is_verbose_mode;
    private boolean autoyes;
    private boolean nowait;

    protected void initialize(boolean is_verbose_mode, boolean autoyes, boolean nowait) {
        this.is_verbose_mode = is_verbose_mode;
        this.autoyes = autoyes;
        this.nowait = nowait;
    }

    /***
     * Trace message unconditionally
     * @param msg
     */
    protected void trace(String msg) {
        System.out.println(msg);
    }

    /***
     * Trace message if -v
     * @param msg
     */
    protected void traceVerbose(String msg) {
        if (is_verbose_mode) {
            System.out.println(msg);
        }
    }

    /**
     * print a message and:
     * - normal mode: wait for a key press by the user, then continue
     * - unattended mode ("-y"): wait for unattendedModeWaitSecs seconds, then continue
     * @param message message to print
     * @param unattendedModeWaitSecs seconds to wait in unattended mode (0 to not wait at all)
     */
    protected void waitForKeyPress(String message, int unattendedModeWaitSecs) {
        if (message != null) {
            System.out.println(message);
        }
        System.out.print("<press key>");
        if (autoyes) {
            System.out.print (" ... (autopress) ");
            if (nowait == false && unattendedModeWaitSecs > 0) {
                System.out.print("... waiting " +unattendedModeWaitSecs + " secs ...");
                try {
                    Thread.sleep(unattendedModeWaitSecs * 1000);
                } catch (InterruptedException e) {
                }
            }
        } else {
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println (" ... continuing.");
    }

    protected void waitForKeyPress(String message) {
        waitForKeyPress(message, 0);
    }

    protected void waitForKeyPress() {
        waitForKeyPress(null);
    }


}
