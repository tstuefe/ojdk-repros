package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ManyLambdas {

    static String prepareCode(int numLambdas) {

        String code =
                "import java.util.ArrayList;" +
                "import java.util.List;" +
                "import java.util.Random;" +
                "import java.util.function.Predicate;" +
                "" +
                "class Person {" +
                "	public String firstname;" +
                "	public String lastname;" +
                "	public int age;" +
                "	public int yearly_income;" +
                "	public Person(String firstname, String lastname, int age, int yearly_income) {" +
                "		super();" +
                "		this.firstname = firstname;" +
                "		this.lastname = lastname;" +
                "		this.age = age;" +
                "		this.yearly_income = yearly_income;" +
                "	}" +
                "" +
                "	static String[] firstNames = new String[] {" +
                "		\"Fred\", \"Richard\", \"David\", \"Donald\", \"Nero\", \"Micki Mouse\", \"Luis\", \"Saar\", \"Roswitha\", \"Erna\", \"Nicole\"" +
                "	};" +
                "" +
                "	static String[] lastNames = new String[] {" +
                "		\"Neiru\", \"Stuefe\", \"Varma\", \"Gödel\", \"Studenbeker\", \"Kant\", \"Heisinger\"" +
                "	};" +
                "" +
                "	static Random rand = new Random();" +
                "" +
                "	static Person createRandomPerson() {" +
                "		return new Person(" +
                "				firstNames[rand.nextInt(firstNames.length)]," +
                "				lastNames[rand.nextInt(lastNames.length)]," +
                "				rand.nextInt(99)," +
                "				rand.nextInt(200000)" +
                "		);" +
                "	}" +
                "}" +
                "" +
                "public class ManyManyLambdas {" +
                "" +
                "	static List<Person> list = new ArrayList<>();" +
                "" +
                "	static void init() {" +
                "		for (int i = 0; i < 1000; i ++) {" +
                "			list.add(Person.createRandomPerson());" +
                "		}" +
                "	}" +
                "" +
                "	static void printPersonWithPredicate(List<Person> list, Predicate<Person> p) {" +
                "		for (Person person: list) {" +
                "			if (p.test(person)) {" +
                "				System.out.println(person.firstname + \" \" + person.lastname + \", \" + person.age + \" years, \" + person.yearly_income + \" income\");" +
                "			}" +
                "		}" +
                "	}" +
                "" +
                "	public static void do() throws Exception {" +
                "		init();" +
                "" +
                "		System.out.println(\"All:\");" +
                "		printPersonWithPredicate(list, person -> true);" +
                "		System.out.println(\"\");" +
                "" +
                "		System.out.println(\"Childs:\");" +
                "		printPersonWithPredicate(list, person -> person.age < 18);" +
                "		System.out.println(\"\");" +
                "" +
                "		System.out.println(\"Rich:\");" +
                "		printPersonWithPredicate(list, person -> person.yearly_income > 100000);" +
                "		System.out.println(\"\");" +
                "" +
                "        XXX" +
                "	}" +
                "" +
                "" +
                "}" +
                "";

        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < numLambdas; i ++) {
            bld.append("printPersonWithPredicate(list, person -> person.age < " + i + ");");
        }

        code = code.replace("XXX", bld.toString());

        //System.out.println(code);

        return code;

    }


    public static void main(String[] args) {

        String code = prepareCode(100);
        Utils.createClassFromSource("ManyManyLambdas", code);

        MiscUtils.waitForKeyPress("Before loading...");

        System.gc();
        MiscUtils.waitForKeyPress();

        InMemoryClassLoader loader = new InMemoryClassLoader("ManyLambdasClassloader", null);

        try {

            Class<?> clazz = Class.forName("ManyManyLambdas", true, loader);

            Method m = clazz.getMethod("do");
            m.invoke(null);

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }


        MiscUtils.waitForKeyPress();


    }

}
