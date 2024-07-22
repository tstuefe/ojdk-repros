package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.TestCaseBase;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "GenerateSources", mixinStandardHelpOptions = true,
        description = "GenerateSources - use it to generate source files for various tests in this package.")
public class GenerateSources implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new GenerateSources()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Option(names = { "--num-classes", "-C" }, description = "Number of classes (default: ${DEFAULT_VALUE})")
    int numClasses=256;

    @CommandLine.Option(names = { "--realistic" },
            description = "Generate classes that have a realistic distribution of sizes (default: ${DEFAULT_VALUE}); if false, objects will have one oop map.")
    boolean realistic=true;

    @CommandLine.Option(names = { "--output-dir", "-D" },
            description = "output directory (default: ${DEFAULT_VALUE})")
    String outputDir = "./generated_sources";

    static class Probability {
        public int value;
        public float probability;

        public Probability(int value, float probability) {
            this.value = value;
            this.probability = probability;
        }
    };

    static Random random;

    // Hardcoded table of probability percentages of IK object sizes (taken from a SPECJBB run)
    static final Probability[] probableObjectSizes = new Probability[] {
            new Probability(16,1.84f),
            new Probability(24,31.99f),
            new Probability(32,67.51f),
            new Probability(40,98.30f),
            new Probability(48,99.71f),
            new Probability(56,99.73f),
            new Probability(64,99.93f),
            new Probability(72,99.95f),
            new Probability(80,99.96f),
            new Probability(88,99.99f),
            new Probability(96,99.99f),
            new Probability(104,100.00f)
    };

    static final int getProbableOopSize() {
        int i = random.nextInt(100000);
        float f = (float)i / 1000.0f;
        for (Probability prob : probableObjectSizes) {
            if (f < prob.probability) {
                return prob.value;
            }
        }
        throw new RuntimeException("?? " + f);
    }

    String generateSource(String classname) {
        StringBuilder bld = new StringBuilder();
        bld.append("public class " + classname + "{\n");

        int size = realistic ? getProbableOopSize() : 40;
        int oopslots = (size - 16) / 4; // we just assume narrowOOp here
        for (int i = 0; i < oopslots; i++) {
            bld.append("public Object o");
            bld.append(i);
            bld.append(";\n");
        }
        bld.append("};\n");
        return bld.toString();
    }

    @Override
    public Integer call() throws Exception {

        System.out.println("Num Classes: " + numClasses);
        System.out.println("realistic: " + realistic);

        random = new Random(0x12345678);

        File packageDir = new File(outputDir);
        packageDir.mkdirs();

        System.out.print("Generate " + numClasses + " class sources into " + packageDir.getName() + "...");
        for (int i = 0; i < numClasses; i ++) {
            String classname = "GeneratedClass" + i;
            String source = generateSource(classname);
            File sourceFile = new File(packageDir.getAbsolutePath() + File.separator + classname + ".java");
            BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile));
            writer.write(source);
            writer.close();
        }

        System.out.println("Done. ");

        return 0;

    }

}
