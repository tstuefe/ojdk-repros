package de.stuefe.repros;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

public class SimpleLambda2 {

	static List<Person> list = new ArrayList<>();

	static void init() {
		for (int i = 0; i < 1000; i ++) {
			list.add(Person.createRandomPerson());
		}
	}
	
	static void printPersonWithPredicate(List<Person> list, Predicate<Person> p) {
		for (Person person: list) {
			if (p.test(person)) {
				System.out.println(person.firstname + " " + person.lastname + ", " + person.age + " years, " + person.yearly_income + " income");
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		init();
		
		System.out.println("All:");
		printPersonWithPredicate(list, person -> true);
		System.out.println("");
				
		System.out.println("Childs:");
		printPersonWithPredicate(list, person -> person.age < 18);
		System.out.println("");

		System.out.println("Rich:");
		printPersonWithPredicate(list, person -> person.yearly_income > 100000);
		System.out.println("");

		MiscUtils.waitForKeyPress();
	}
	
	

}
