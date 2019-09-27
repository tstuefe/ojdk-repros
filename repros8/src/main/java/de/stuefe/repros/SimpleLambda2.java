package de.stuefe.repros;


import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

class Person {
	public String firstname;
	public String lastname;
	public int age;
	public int yearly_income;
	public Person(String firstname, String lastname, int age, int yearly_income) {
		super();
		this.firstname = firstname;
		this.lastname = lastname;
		this.age = age;
		this.yearly_income = yearly_income;
	}

	static String[] firstNames = new String[] {
		"Fred", "Richard", "David", "Donald", "Nero", "Micki Mouse", "Luis", "Saar", "Roswitha", "Erna", "Nicole"
	};

	static String[] lastNames = new String[] {
		"Neiru", "Stuefe", "Varma", "Gödel", "Studenbeker", "Kant", "Heisinger"
	};

	static Random rand = new Random();

	static Person createRandomPerson() {
		return new Person(
				firstNames[rand.nextInt(firstNames.length)],
				lastNames[rand.nextInt(lastNames.length)],
				rand.nextInt(99),
				rand.nextInt(200000)
		);
	}
}

@CommandLine.Command(name = "SimpleLambda2", mixinStandardHelpOptions = true)
public class SimpleLambda2 extends TestCaseBase implements Callable<Integer> {

	@CommandLine.Option(names = { "--auto-yes", "-y" }, defaultValue = "false",
			description = "Autoyes.")
	boolean auto_yes;
	int unattendedModeWaitSecs = 4;

	@CommandLine.Option(names = { "--verbose", "-v" }, defaultValue = "false",
			description = "Verbose.")
	boolean verbose;


	public static void main(String... args) {
		int exitCode = new CommandLine(new SimpleLambda2()).execute(args);
		System.exit(exitCode);
	}



	static List<Person> list = new ArrayList<>();

	static void init() {
		list.clear();
		for (int i = 0; i < 1000; i ++) {
			list.add(Person.createRandomPerson());
		}
	}
	
	static int countPersonWithPredicate(List<Person> list, Predicate<Person> p) {
		int i = 0;
		for (Person person: list) {
			if (p.test(person)) {
				i ++;
				//System.out.println(person.firstname + " " + person.lastname + ", " + person.age + " years, " + person.yearly_income + " income");
			}
		}
		return i;
	}

	@CommandLine.Option(names = { "--repeat", "-n" }, defaultValue = "100",
			description = "repeat count.")
	int repeat_count;

	@Override
	public Integer call() throws Exception {

		initialize(verbose, auto_yes);

		for (int run = 0; run < repeat_count; run ++) {
			init();
			int i = 0;
			i = countPersonWithPredicate(list, person -> true);
			System.out.println("All:" + i);

			i = countPersonWithPredicate(list, person -> person.age < 18);
			System.out.println("Childs:" + i);

			i = countPersonWithPredicate(list, person -> person.yearly_income > 100000);
			System.out.println("Rich:" + i);

			int j = 0;
			j+= countPersonWithPredicate(list, person -> person.yearly_income < 10000);
			j+= countPersonWithPredicate(list, person -> person.yearly_income < 9999);
			j+= countPersonWithPredicate(list, person -> person.yearly_income < 9998);
			j+= countPersonWithPredicate(list, person -> person.yearly_income < 9997);
			System.out.println("Poor:" + j);

			waitForKeyPress();
		}

		return 0;
	}
	
	

}
