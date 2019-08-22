package de.stuefe.repros.util;

import org.apache.commons.cli.*;

import java.io.IOException;

public abstract class MyTestCaseBase {

    private Options options;

    protected CommandLine cmdline;

    protected String[] positionalArguments;

    protected boolean is_verbose_mode = false;

    protected boolean is_unattended_mode = false;

    /**
     * child class shall call this first in main(). Will parse commandline,
     * handle standard stuff like -h and -v.
     * After returning, cmdline contains the parsed command line,
     * and positionalArguments is an array of the positional args.
     * @param me calling class
     * @param args args
     * @param testSpecificOptions options specific to this test
     */
    protected void prolog(Class me, String[] args, Option[] testSpecificOptions) {
        prolog(me, args, testSpecificOptions, null);
    }

    /**
     * child class shall call this first in main(). Will parse commandline,
     * handle standard stuff like -h and -v.
     * After returning, cmdline contains the parsed command line,
     * and positionalArguments is an array of the positional args.
     * @param me calling class
     * @param args args
     * @param testSpecificOptions options specific to this test
     * @param positionalArgsText optional, text to display
     */
    protected void prolog(Class me, String[] args, Option[] testSpecificOptions,
                          String positionalArgsText)
    {

        options = CommandLineHelpers.prepareOptions();

        Option unattended_mode = Option.builder("y")
                .longOpt("auto-press-yes")
                .desc("automatically key press (unattended mode)")
                .hasArg(false)
                .build();
        options.addOption(unattended_mode);

        for (Option opt : testSpecificOptions) {
            options.addOption(opt);
        }

        CommandLineParser parser = new DefaultParser();

        try {
            cmdline = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        CommandLineHelpers.handleHelpForClass(cmdline, getClass(), options,
                positionalArgsText);

        if (cmdline.hasOption('y')) {
            is_unattended_mode = true;
        }

        if (cmdline.hasOption('v')) {
            is_verbose_mode = true;
        }

        // All remaining args are positional
        positionalArguments = cmdline.getArgs();
        if (positionalArguments == null) {
            positionalArguments = new String[] {};
        }

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
        if (is_unattended_mode) {
            System.out.print (" ... (autopress) ");
            if (unattendedModeWaitSecs > 0) {
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
