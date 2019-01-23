package de.stuefe.repros;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

class Emperor {
	public Emperor(String name, int year_from, int year_to, boolean was_insane, boolean was_awesome,
			boolean was_murdered) {
		super();
		this.name = name;
		this.year_from = year_from;
		this.year_to = year_to;
		this.was_insane = was_insane;
		this.was_awesome = was_awesome;
		this.was_murdered = was_murdered;
	}
	public String name;
	int year_from;
	int year_to;
	
	int years() {
		return Math.abs(year_from - year_to);
	}
	
	boolean was_insane;
	boolean was_awesome;
	boolean was_murdered;
}


public class SimpleLambda {
	static List<Emperor> list = new ArrayList<Emperor>();
	static void init() {
		list.add(new Emperor("Julius Caesar", -49, -44, false, true, true));
		list.add(new Emperor("Octavius (Augustus)", -27, 14, false, true, false));
		list.add(new Emperor("Tiberius", 14, 37, true, false, false));
		list.add(new Emperor("Caligula", 37, 41, true, false, true));
		list.add(new Emperor("Claudius", 41, 54, false, false, true));
		list.add(new Emperor("Nero", 54, 68, true, false, true));
		
		list.add(new Emperor("Galba", 68, 69, false, false, true));
		list.add(new Emperor("Otho", 69, 69, false, false, true));
		list.add(new Emperor("Vitellius", 69, 69, false, false, true));
		
		list.add(new Emperor("Vespasian", 69, 79, false, true, false));
		list.add(new Emperor("Titus", 79, 81, false, false, false));
		list.add(new Emperor("Domitian", 81, 96, false, false, true));
		
		list.add(new Emperor("Nerva", 96, 98, false, false, false));
		
		list.add(new Emperor("Trajan", 98, 117, false, true, false));
	}
	
	static void printEmperorsWithPredicate(List<Emperor> list, Predicate<Emperor> p) {
		for (Emperor e: list) {
			if (p.test(e)) {
				System.out.println(e.name + " - reigned " + e.years() + " years.");
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		init();
		
		System.out.println("All:");
		printEmperorsWithPredicate(list, e -> true);
		System.out.println("");
				
		System.out.println("Insane:");
		printEmperorsWithPredicate(list, e -> e.was_insane);
		System.out.println("");
		
		System.out.println("Awesome:");
		printEmperorsWithPredicate(list, e -> e.was_awesome);
		System.out.println("");
		
		System.out.println("Mayflies:");
		printEmperorsWithPredicate(list, (Emperor e) -> e.years() <= 2);
		System.out.println("");
		
		System.out.println("<press key>");
		System.in.read();
		
	}
	
	

}
