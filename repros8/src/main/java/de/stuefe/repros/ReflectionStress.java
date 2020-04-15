package de.stuefe.repros;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import de.stuefe.repros.metaspace.internals.InMemoryJavaFileManager;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

@CommandLine.Command(name = "ReflectionStress", mixinStandardHelpOptions = true,
        description = "Test reflection.")
public class ReflectionStress implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "Number of classes.")
    int numClasses;

    @CommandLine.Parameters(index = "0", description = "Size of classes (default 5).")
    int sizeClasses = 5;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ReflectionStress()).execute(args);
        System.exit(exitCode);
    }


    private static void createRandomClass(String classname, int sizeFactor) {
        String code = Utils.makeRandomSource(sizeFactor).replaceAll("CLASSNAME", classname);
        boolean success = InMemoryJavaFileManager.theFileManager().compileSingleFile(classname, code);
        assert(success);
    }

    private static String nameClass(int number, int sizeFactor) {
        return "myclass_size_" + sizeFactor + "_number_" + number;
    }

    private static void generateClasses(int numClasses, int sizeFactor) {
        for (int i = 0; i < numClasses; i++) {
            String className = nameClass(i, sizeFactor);
            createRandomClass(className, sizeFactor);
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
        }
    }

    @Override
    public Integer call() throws Exception {

        // Generate n random classes named "myclass<i>". Each shall have one method for
        // public int get_i<n>(), public byte get_b<n>(), public String get_s<n>()
        // Invoke them each 100 times.


        System.out.print("Generate in memory class files...");
        System.out.print("(" + numClasses + ") ...");
        generateClasses(numClasses, sizeClasses);
        System.out.print("Done.");

        System.out.println("<press key>");
        System.in.read();

        for (int run = 0; run < 1000; run++) {

            InMemoryClassLoader cl = new InMemoryClassLoader();

            // load small classes
            System.out.print("Load " + numClasses + " classes...");
            for (int i = 0; i < numClasses; i++) {
                String className = nameClass(i, sizeClasses);
                Class<?> clazz = Class.forName(className, true, cl);
                if (i % 100 == 0) {
                    System.out.println(i + "...");
                }
            }
            System.out.print("Done.");

            System.out.println("<press key>");

            System.out.print("Invoking...");

            // Now start reflection
            for (int i = 0; i < numClasses; i++) {
                String className = nameClass(i, sizeClasses);
                Class<?> clazz = cl.loadClass(className);
                Object o = clazz.getDeclaredConstructor().newInstance();
                Method[] allMethods = clazz.getDeclaredMethods();
                for (Method m : allMethods) {
                    for (int j = 0; j < 100; j++) {
                        m.invoke(o);
                    }
                }
                Field[] allFields = clazz.getDeclaredFields();
                for (Field f : allFields) {
                    for (int j = 0; j < 100; j++) {
                        f.get(o);
                    }
                }
                if (i % 100 == 0) {
                    System.out.println(i + "...");
                }
            }
            System.out.print("Done...");

            System.out.println("<press key>");
            System.in.read();

            // clean all up
            System.gc();

            System.out.println("system gced");
            System.out.println("<press key>");
            System.in.read();

        }

        return 0;
    }

}
