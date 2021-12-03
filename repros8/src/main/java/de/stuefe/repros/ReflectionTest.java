package de.stuefe.repros;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

import de.stuefe.repros.metaspace.internals.InMemoryJavaFileManager;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

@CommandLine.Command(name = "ReflectionStress", mixinStandardHelpOptions = true,
        description = "Test reflection.")
public class ReflectionTest extends TestCaseBase implements Callable<Integer> {

    @CommandLine.Option(names = {"--num-classes"},
            description = "Number of classes (default: ${DEFAULT-VALUE})")
    int num_classes = 1000;

    @CommandLine.Option(names = {"--size-classes"},
            description = "Number of classes (default: ${DEFAULT-VALUE}, negative value: randomize from [0...n*2).)")
    int size_classes = 5;
    boolean randomize_class_size = false;

    @CommandLine.Option(names = {"--cycles"},
            description = "Number of load/unload cycles (default: ${DEFAULT-VALUE})")
    int cycles = 3;

    @CommandLine.Option(names = {"--invocs"},
            description = "Number of invocations per cycle (default: ${DEFAULT-VALUE})")
    int invocations_per_cycle = 20;

    @CommandLine.Option(names = {"--gc-after-cycle"},
            description = "After a cycle, do a gc? (default: ${DEFAULT-VALUE})")
    boolean gc_each_cycle = true;

    @CommandLine.Option(names = {"--auto-yes", "-y"},
            description = "Autoyes (default: ${DEFAULT-VALUE})")
    boolean auto_yes = false;

    @CommandLine.Option(names = {"--nowait"},
            description = "do not wait (only with autoyes) (default: ${DEFAULT-VALUE})")
    boolean nowait = false;

    @CommandLine.Option(names = {"--verbose"},
            description = "verbose mode (only with autoyes) (default: ${DEFAULT-VALUE})")
    boolean verbose = false;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ReflectionTest()).execute(args);
        System.exit(exitCode);
    }

    private static void createRandomClass(String classname, int sizeFactor) {
        String code = Utils.makeRandomSource(sizeFactor).replaceAll("CLASSNAME", classname);
        boolean success = InMemoryJavaFileManager.theFileManager().compileSingleFile(classname, code);
        assert (success);
    }

    private static String nameClass(int number, int sizeFactor) {
        return "myclass_size_" + sizeFactor + "_number_" + number;
    }

    static java.util.Random rand = new Random();

    private void generateClasses(int numClasses, int sizeFactor) {
        ProgressPrinter pp = new ProgressPrinter();
        for (int i = 0; i < numClasses; i++) {
            String className = nameClass(i, sizeFactor);
            int size = sizeFactor;
            if (randomize_class_size) {
                size = 1 + rand.nextInt(size * 2);
            }
            createRandomClass(className, size);
            pp.inc();
        }
        pp.done();
    }

    @Override
    public Integer call() throws Exception {

        if (size_classes < 0) {
            size_classes = -size_classes;
            randomize_class_size = true;
        }

        System.out.println("num_classes: " + num_classes);
        System.out.println("size_classes: " + size_classes + (randomize_class_size ? "-ish" : ""));
        System.out.println("cycles: " + cycles);
        System.out.println("invocations_per_cycle: " + invocations_per_cycle);
        System.out.println("gc_each_cycle: " + gc_each_cycle);

        initialize(verbose, auto_yes, nowait);

        // Generate n random classes named "myclass<i>". Each shall have one method for
        // public int get_i<n>(), public byte get_b<n>(), public String get_s<n>()
        // Invoke them each 100 times.

        System.out.print("Generate in memory class files...");
        System.out.print("(" + num_classes + ") ...");
        generateClasses(num_classes, size_classes);

        waitForKeyPress("Before loading...");

        InMemoryClassLoader cl = new InMemoryClassLoader();

        // create and load classes
        ArrayList<Class<?>> classes = new ArrayList<>(num_classes);

        {
            ProgressPrinter pp = new ProgressPrinter();
            System.out.print("Load " + num_classes + " classes...");
            for (int i = 0; i < num_classes; i++) {
                String className = nameClass(i, size_classes);
                Class<?> clazz = Class.forName(className, true, cl);
                pp.inc();
                classes.add(clazz);
            }
            pp.done();
            waitForKeyPress("Done loading classes");
        }

        // Now start reflection
        for (int cycle = 0; cycle < cycles; cycle++) {
            waitForKeyPress("before cycle " + cycle);

            // Cache Method and Constructors and throw them away at
            // the end of the cycle.
            ArrayList reflectors = new ArrayList();

            ProgressPrinter pp = new ProgressPrinter();
            for (Class<?> clazz : classes) {
                Constructor ctor = clazz.getDeclaredConstructor();
                reflectors.add(ctor);
                Method[] allMethods = clazz.getDeclaredMethods();
                for (Method m : allMethods) {
                    reflectors.add(m);
                }

                Object o = ctor.newInstance();
                for (Method m : allMethods) {
                    for (int j = 0; j < invocations_per_cycle; j++) {
                        m.invoke(o);
                    }
                }
                Field[] allFields = clazz.getDeclaredFields();
                for (Field f : allFields) {
                    for (int j = 0; j < invocations_per_cycle; j++) {
                        f.get(o);
                    }
                }
                pp.inc();
            }
            pp.done();
            if (gc_each_cycle) {
                waitForKeyPress("before GC" + cycle);
                reflectors = null; // let go of Method and Constructor objects
                System.gc();
                System.gc();
                System.gc();
            }
            waitForKeyPress("after cycle " + cycle);
        }

        // let go of initial class loader and all classes
        waitForKeyPress("before final GC");
        cl = null;
        classes = null;
        System.gc(); System.gc();
        waitForKeyPress("after final GC");

        return 0;
    }

}
