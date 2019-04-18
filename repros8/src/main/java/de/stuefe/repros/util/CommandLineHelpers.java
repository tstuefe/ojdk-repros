package de.stuefe.repros.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CommandLineHelpers {


    /***
     * Produce a standard options set with -h and -v already filled in
     * @return options
     */
    public static Options prepareOptions() {

        Options o = new Options();

        Option help_option = new Option("h", "help",  false,"print this message");
        o.addOption(help_option);

        Option verbose_option = new Option("v", "verbose",  false,"verbose mode");
        o.addOption(verbose_option);

        return o;

    }

    public static void handleHelpForClass(CommandLine cmdline, Class mainClass, Options options) {
        handleHelpForClass(cmdline, mainClass, options, null);
    }

    public static void handleHelpForClass(CommandLine cmdline, Class mainClass, Options options, String positionalArgs) {
        if (cmdline.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            String cmdLineSyntax = mainClass.getName() + " [OPTION]";
            if (positionalArgs != null) {
                cmdLineSyntax += " " + positionalArgs;
            }
            formatter.printHelp(cmdLineSyntax, null, options, null);
            System.exit(0);
        }
    }

}
