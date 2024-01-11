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

    @CommandLine.Option(names = { "--autoyes", "-y" }, defaultValue = "false",
            description = "Autoyes.")
    boolean auto_yes;
    int unattendedModeWaitSecs = 4;

    @CommandLine.Option(names = { "--nowait" }, defaultValue = "false",
            description = "do not wait (only with autoyes).")
    boolean nowait;

    @CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
            description = "Verbose.")
    boolean verbose;

    public static void main(String... args) {
        int exitCode = new CommandLine(new LoadAllClasses()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Option(names = { "--ignore-errors", "-i" }, defaultValue = "false",
            description = "Ignore load errors.")
    boolean ignoreErrors;

    @CommandLine.Parameters(index = "0..*", arity = "1")
    List<String> files;

    @Override
    public Integer call() throws Exception {
        initialize(verbose, auto_yes, nowait);

        int loadedTotal = 0;
        int jarsTotal = 0;

        for (String s : files) {
            System.out.println("Load all classes from: " + s);

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

            jarsTotal ++;

            boolean isJmod = s.endsWith(".jmod");

            Enumeration<JarEntry> e = jarFile.entries();

            URL[] urls = { new URL("jar:file:" + s + "!/") };
            URLClassLoader cl = URLClassLoader.newInstance(urls);

            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                System.out.print(je.getName());
                if (je.isDirectory() || !je.getName().endsWith(".class")) {
                    System.out.println(" (omitted)");
                    continue;
                }
                if (isJmod && !je.getName().startsWith("classes/")) {
                    System.out.println(" (omitted)");
                    continue;
                }
                String className = je.getName().substring(0,je.getName().length() - 6 /* cut ".class" */ );
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
                        System.out.println(" (failed)");
                        continue;
                    }
                }
                if (c != null) {
                    System.out.println(" (ok)");
                    loadedTotal ++;
                } else {
                    System.out.println(" (null?)");
                }
            }
        }
        System.out.println("Loaded : " + loadedTotal + " classes from " + jarsTotal + " jar file(s)");
        return 0;
    }
}
