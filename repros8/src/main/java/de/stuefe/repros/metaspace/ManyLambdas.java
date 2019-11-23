package de.stuefe.repros.metaspace;


import de.stuefe.repros.TestCaseBase;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "ManyLambdas", mixinStandardHelpOptions = true,
        description = "ManyLambdas repro.")
public class ManyLambdas
        extends TestCaseBase implements Callable<Integer> {

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
        int exitCode = new CommandLine(new ManyLambdas()).execute(args);
        System.exit(exitCode);
    }

    static String codeForClassWithManyLambdas(String classname, int numLambdas) {

        String code =
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n" +
                "import java.util.Random;\n" +
                "import java.util.function.Predicate;\n" +
                "\n" +
                "public class " + classname + "{\n" +
                "\n" +
                "static class Person {\n" +
                "	public String firstname;\n" +
                "	public String lastname;\n" +
                "	public int age;\n" +
                "	public int yearly_income;\n" +
                "	public Person(String firstname, String lastname, int age, int yearly_income) {\n" +
                "		super();\n" +
                "		this.firstname = firstname;\n" +
                "		this.lastname = lastname;\n" +
                "		this.age = age;\n" +
                "		this.yearly_income = yearly_income;\n" +
                "	}\n" +
                "\n" +
                "	static String[] firstNames = new String[] {\n" +
                "		\"Fred\", \"Richard\", \"David\", \"Donald\", \"Nero\", \"Micki Mouse\", \"Luis\", \"Saar\", \"Roswitha\", \"Erna\", \"Nicole\"" +
                "	}\n;" +
                "\n" +
                "	static String[] lastNames = new String[] {\n" +
                "		\"Neiru\", \"Stuefe\", \"Varma\", \"Gödel\", \"Studenbeker\", \"Kant\", \"Heisinger\"" +
                "	};\n" +
                "\n" +
                "	static Random rand = new Random();\n" +
                "\n" +
                "	static Person createRandomPerson() {\n" +
                "		return new Person(\n" +
                "				firstNames[rand.nextInt(firstNames.length)],\n" +
                "				lastNames[rand.nextInt(lastNames.length)],\n" +
                "				rand.nextInt(99),\n" +
                "				rand.nextInt(200000)\n" +
                "		);\n" +
                "	}\n" +
                "}\n" +
                "\n" +
                "	static List<Person> list = new ArrayList<>();\n" +
                "\n" +
                "	static void init() {\n" +
                "		for (int i = 0; i < 1000; i ++) {\n" +
                "			list.add(Person.createRandomPerson());\n" +
                "		}\n" +
                "	}\n" +
                "\n" +
                "	static int countPersonWithPredicate(List<Person> list, Predicate<Person> p) {\n" +
                "       int cnt = 0; \n " +
                "		for (Person person: list) {\n" +
                "			if (p.test(person)) {\n" +
                "				cnt++;\n" +
          //      "				System.out.println(person.firstname + \" \" + person.lastname + \", \" + person.age + \" years, \" + person.yearly_income + \" income\");" +
                "			}\n" +
                "		}\n" +
                "       return cnt; \n" +
                "	}\n" +
                "\n" +
                "	public static int doit() throws Exception {\n" +
                "		init();\n" +
                "\n" +
                "		int n = 0;\n" +
                "       XXX\n" +
                "       return n;\n" +
                "	}\n" +
                "\n" +
                "}\n" +
                "";

        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < numLambdas; i ++) {
            switch (i % 3) {
                case 0: bld.append("n += countPersonWithPredicate(list, person -> true);\n"); break;
                case 1: bld.append("n += countPersonWithPredicate(list, person -> person.age < " + i + ");\n"); break;
                case 2: bld.append("n += countPersonWithPredicate(list, person -> person.yearly_income > " + i * 1000 + ");\n"); break;
            }
        }

        code = code.replace("XXX", bld.toString());

        //System.out.println(code);

        return code;

    }


    @CommandLine.Option(names = { "--num-lambdas" }, defaultValue = "2300",
            description = "Number of lambdas.")
    int num_lambdas;

    @Override
    public Integer call() throws Exception {

        initialize(verbose, auto_yes, nowait);

        waitForKeyPress("Before loading...");

        int max_lambdas_per_class = 1000;
        ArrayList<Class> classes = new ArrayList<>();
        InMemoryClassLoader loader = new InMemoryClassLoader("ManyLambdasClassloader", null);

        for (int n = num_lambdas; n > 0; n -= max_lambdas_per_class) {
            System.out.print("*");
            String className = "ManyManyLambdas" + n;
            int lambdas = n > max_lambdas_per_class ? max_lambdas_per_class : n;
            String code = codeForClassWithManyLambdas(className, lambdas);
            Utils.createClassFromSource(className, code);
            classes.add(Class.forName(className, true, loader));
        }

        System.out.println();
        System.gc();
        waitForKeyPress("Before invoking...");

        int n = 0;
        for (Class c : classes) {
            System.out.print("*");
            Method m = c.getMethod("doit");
            n += ((Integer)m.invoke(null)).intValue();
        }

        System.out.println();
        waitForKeyPress("After invoking...");

        return 0;

    }

}
