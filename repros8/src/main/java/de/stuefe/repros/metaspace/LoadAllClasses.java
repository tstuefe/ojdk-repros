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
    int repeat = 1;

    @CommandLine.Parameters(index = "0..*", arity = "1")
    List<String> files;

    public static ArrayList<ClassLoader> loaders = new ArrayList<>();
    public static ArrayList<Class> classes = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        int loadedTotal = 0, errorsTotal = 0, omittedTotal = 0;

        waitForKeyPress("Before start...");

        for (int i = 0; i < repeat; i++) {  // for every loader
            traceVerbose("Iteration " + i + "...");
            for (String s : files) { // for every jar file
                traceVerbose("Load all classes from: " + s);
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(s);
                } catch (IOException ex) {
                    trace("Failed to open jar " + s);
                    if (!ignoreErrors) {
                        ex.printStackTrace();
                        return -1;
                    } else {
                        continue;
                    }
                }

                int loaded0 = 0, errors0 = 0, omitted0 = 0;

                boolean isJmod = s.endsWith(".jmod");

                Enumeration<JarEntry> e = jarFile.entries();

                URL[] urls = { new URL("jar:file:" + s + "!/") };
                URLClassLoader cl = new URLClassLoader(urls, null /* dont search parent */);
                loaders.add(cl);

                while (e.hasMoreElements()) {
                    JarEntry je = e.nextElement();
                    if (je.isDirectory() || !je.getName().endsWith(".class")) {
                        omitted0 ++;
                        continue;
                    }
                    if (isJmod && !je.getName().startsWith("classes/")) {
                        omitted0 ++;
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
                        loaded0++;
                        classes.add(c);
                    } catch (OutOfMemoryError ex) {
                      ex.printStackTrace();
                      trace("Total classes loaded: " + loadedTotal + ", omitted: " + omittedTotal + ", errors: " + errorsTotal);
                      waitForKeyPress("Done (OOM)");
                      System.exit(-1);
                    } catch (ClassNotFoundException|NoClassDefFoundError ex) {
                        errors0++;
                        //traceVerbose(" Failed to load " + className);
                        if (!ignoreErrors) {
                            ex.printStackTrace();
                            return -1;
                        }
                    }
                }
                traceVerbose("... classes loaded: " + loaded0 + ", omitted: " + omitted0 + ", errors: " + errors0);
                loadedTotal += loaded0;
                omittedTotal += omitted0;
                errorsTotal += errors0;
            }  // for every jar file
        }  // for every loader

        trace("Total classes loaded: " + loadedTotal + ", omitted: " + omittedTotal + ", errors: " + errorsTotal);

        waitForKeyPress("Done");

        return 0;
    }
}
