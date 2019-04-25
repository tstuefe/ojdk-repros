package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import de.stuefe.repros.util.MyTestCaseBase;
import org.apache.commons.cli.Option;

import java.util.ArrayList;

public class ClassLifeCycle extends MyTestCaseBase {

    public static void main(String args[]) throws Exception {
        ClassLifeCycle test = new ClassLifeCycle();
        test.run(args);
    }

    Option options_num_classes =
            Option.builder()
                    .longOpt("num-classes-per-loader")
                    .hasArg().type(Long.class)
                    .desc("number of classes per loader")
                    .build();

    Option options_size_classes =
            Option.builder()
                    .longOpt("class-size")
                    .hasArg().type(Long.class)
                    .desc("avg class size factor")
                    .build();

    class LoaderGeneration {
        ArrayList<ClassLoader> loaders = new ArrayList<>();
        ArrayList<Class> loaded_classes = new ArrayList<>();
    }

    ;

    private void run(String[] args) throws Exception {

        Option[] options = new Option[]{
                options_num_classes,
                options_size_classes};

        prolog(getClass(), args, options);

        int numClasses = Integer.parseInt(options_num_classes.getValue("100"));
        int sizeFactor = Integer.parseInt(options_size_classes.getValue("10"));

        System.out.print("Generating " + numClasses + " in memory class files, size factor "  + sizeFactor + " ...");
        for (int j = 0; j < numClasses; j++) {
            String className = "testclass_" + j;
            Utils.createRandomClass(className, sizeFactor);
        }

        waitForKeyPress("Before creating loader...");

        ClassLoader loader = new InMemoryClassLoader("myloader", null);

        waitForKeyPress("Before loading...");

        for (int i = 0; i < numClasses; i ++) {
            // Loading classes...
            Class<?> clazz = Class.forName("testclass_" + i, true, loader);
        }

        waitForKeyPress("After loading");

        loader = null;

        waitForKeyPress("Before gc");

        System.gc(); System.gc();
        waitForKeyPress("After gc");

    }


}
