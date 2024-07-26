package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.TestCaseBase;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.io.*;
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

    // Hardcoded table of probability percentages of number of OMB blocks (taken from a SPECJBB run)
    // (strictly speaking, this is number of OMB blocks in unadultered JDK. Lilliput KLUT version
    // reshuffles OMBs to merge them into fewer larger regions)
    static final Probability[] probableOMBNumbers = new Probability[] {
            new Probability(0,40.00f),
            new Probability(1,85.00f),
            new Probability(2,94.00f),
            new Probability(3,97.00f),
            new Probability(4,99.00f),
            new Probability(5,99.7f),
            new Probability(6,100.00f)
    };

    static final int getProbableOopMapBlockNumber() {
        int i = random.nextInt(100000);
        float f = (float)i / 1000.0f;
        for (Probability prob : probableOMBNumbers) {
            if (f < prob.probability) {
                return prob.value;
            }
        }
        throw new RuntimeException("?? " + f);
    }

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

        // How many OMBs should this object have (note, without considering reordering)
        int numOMBs = getProbableOopMapBlockNumber();

        int numOopsInThisObject = 0;
        String baseClass = "java.lang.Object";

        if (numOMBs >= 1) {
            numOopsInThisObject = 1;
            baseClass = "DERIVED" + (numOMBs - 1);
        }

        bld.append("public class " + classname + " extends " + baseClass + " implements IsGenerated {\n");

        int size = realistic ? getProbableOopSize() : 40;
        int numVars = (size - 16) / 4; // we just assume narrowOOp here
        if (numVars > numOopsInThisObject) {
            numVars = numOopsInThisObject;
        }
        int numNonOopVars = numVars - numOopsInThisObject;
        for (int i = 0; i < numNonOopVars; i++) {
            bld.append("public int i" + i + ";\n");
        }
        for (int i = 0; i < numOopsInThisObject; i++) {
            bld.append("public Object o" + i + ";\n");
        }
        bld.append("};\n");
        return bld.toString();
    }

    String generateBaseClassSource(String classname, String baseclass, String field_prefix, int numOops, int numNonOops) {
        StringBuilder bld = new StringBuilder();
        bld.append("public class " + classname + " extends " + baseclass + " implements IsGenerated {\n");
        for (int i = 0; i < numOops; i++) {
            bld.append("public Object " + field_prefix + "_o_" + i + ";\n");
        }
        for (int i = 0; i < numNonOops; i++) {
            bld.append("public int " + field_prefix + "_I_" + i + ";\n");
        }
        bld.append("};\n");
        return bld.toString();
    }

    void writeJavaFile(File packageDir, String classname, String source) throws IOException {
        File sourceFile = new File(packageDir.getAbsolutePath() + File.separator + classname + ".java");
        BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile));
        writer.write(source);
        writer.close();
    }

    void generateAndWriteBaseClass(File packageDir, String classname, String baseclass, String field_prefix, int numOops, int numNonOops) throws IOException {
        String source = generateBaseClassSource(classname, baseclass, field_prefix, numOops, numNonOops);
        writeJavaFile(packageDir, classname, source);
    }

    @Override
    public Integer call() throws Exception {

        System.out.println("Num Classes: " + numClasses);
        System.out.println("realistic: " + realistic);

        random = new Random();

        File packageDir = new File(outputDir);
        packageDir.mkdirs();

        System.out.print("Generate " + numClasses + " class sources into " + packageDir.getName() + "...");

        // Generate base classes first...

        // Marker interface to recognize all objects
        writeJavaFile(packageDir, "IsGenerated", "public interface IsGenerated {}");

        for (int level = 0; level < 10; level++) {
            String baseclass = level > 0 ? "DERIVED" + (level - 1) : "java.lang.Object";
            String thisclass = "DERIVED" + level;
            generateAndWriteBaseClass(packageDir,
                    thisclass, baseclass, thisclass.toLowerCase(), 1, 1);
        }

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
