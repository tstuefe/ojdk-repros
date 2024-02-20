package de.stuefe.repros.metaspace;

import de.stuefe.repros.TestCaseBase;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.List;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "LoadAllClasses", mixinStandardHelpOptions = true,
        description = "LoadAllClasses repro.")
public class LoadAllClasses extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = { "--autoyes", "-y" },
            description = "Auto-Yes (default: ${DEFAULT_VALUE})).)")
    boolean auto_yes;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" },
            description = "do not wait (only with autoyes) (default: ${DEFAULT_VALUE})).")
    boolean nowait = false;

    @CommandLine.Option(names = { "--verbose", "-v" }, description = "Verbose (default: ${DEFAULT_VALUE})")
    boolean verbose = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new LoadAllClasses()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Option(names = { "--ignore-errors", "-i" },
            description = "Ignore load errors (default: ${DEFAULT_VALUE})")
    boolean ignoreErrors = false;

    @CommandLine.Option(names = { "--repeat", "-r" },
            description = "Repeat loading with different loaders (default: ${DEFAULT_VALUE})")
    int numLoaders = 1;

    @CommandLine.Parameters(index = "0..*", arity = "1")
    List<String> files;

    void log(String s) {
        if (verbose) {
            System.out.print(s);
        }
    }

    void log_cr(String s) {
        if (verbose) {
            System.out.println(s);
        }
    }

    final static ArrayList<ClassLoader> loaders = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        int loadedTotal = 0;

        waitForKeyPress("Before start...");

        for (int i = 0; i < numLoaders; i++) {
            for (String s : files) {
                log_cr("Load all classes from: " + s);
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(s);
                } catch (IOException ex) {
                    System.err.println("Failed to open jar " + s);
                    if (!ignoreErrors) {
                        ex.printStackTrace();
                        return -1;
                    } else {
                        continue;
                    }
                }

                boolean isJmod = s.endsWith(".jmod");

                Enumeration<JarEntry> e = jarFile.entries();

                URL[] urls = { new URL("jar:file:" + s + "!/") };
                URLClassLoader cl = new URLClassLoader(urls, null /* dont search parent */);
                loaders.add(cl);

                while (e.hasMoreElements()) {
                    JarEntry je = e.nextElement();
                    log(je.getName());
                    if (je.isDirectory() || !je.getName().endsWith(".class")) {
                        log_cr(" (omitted)");
                        continue;
                    }
                    if (isJmod && !je.getName().startsWith("classes/")) {
                        log_cr(" (omitted)");
                        continue;
                    }
                    String className = je.getName().substring(0, je.getName().length() - 6 /* cut ".class" */);
                    if (isJmod) {
                        // cut leading "classes/"
                        className = className.substring(8);
                    }
                    className = className.replace('/', '.');
                    Class c = null;
                    try {
                        c = cl.loadClass(className);
                    } catch (Throwable ex) {
                        if (!ignoreErrors) {
                            ex.printStackTrace();
                            return -1;
                        } else {
                            log_cr(" (failed)");
                            continue;
                        }
                    }
                    if (c != null) {
                        log_cr(" (ok)");
                        loadedTotal++;
                    } else {
                        log_cr(" (null?)");
                    }
                }
            }
        }

        waitForKeyPress("Loaded : " + loadedTotal + " classes into " + loaders.size() + " loaders.");

        waitForKeyPress("Loaded : " + loadedTotal + " classes into " + loaders.size() + " loaders.");

        return 0;
    }
}
