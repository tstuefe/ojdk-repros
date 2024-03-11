package de.stuefe.repros;

import java.io.IOException;

public class TestCaseBase {

    private boolean is_verbose_mode;
    private boolean autoyes;
    private boolean nowait;
    private int unattendedWaitSecs;

    protected void initialize(boolean is_verbose_mode, boolean autoyes, boolean nowait, int unattendedWaitSecs) {
        this.is_verbose_mode = is_verbose_mode;
        this.autoyes = autoyes;
        this.nowait = nowait;
        this.unattendedWaitSecs = unattendedWaitSecs;
    }

    protected void initialize(boolean is_verbose_mode, boolean autoyes, boolean nowait) {
        initialize(is_verbose_mode, autoyes, nowait, 4);
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
        waitForKeyPress(message, unattendedWaitSecs);
    }

    protected void waitForKeyPress() {
        waitForKeyPress(null);
    }


}
