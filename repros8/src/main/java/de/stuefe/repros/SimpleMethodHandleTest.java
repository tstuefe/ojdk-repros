package de.stuefe.repros;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;


public class SimpleMethodHandleTest {

	public class X {
		public X(){}
		int i = 0;
		public int add(int j) { i += j; return i; }
	};
	
	public static void main(String[] args) throws Throwable {
		MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
		
		MethodType mt = MethodType.methodType(int.class, int.class);
		MethodHandle addMH = publicLookup.findVirtual(X.class, "add", mt);
		
		MethodType voidMT = MethodType.methodType(void.class);
		MethodHandle ctorMH = publicLookup.findConstructor(X.class, voidMT);
		
		X x = (X) ctorMH.invoke();
		int k = (int) addMH.invoke(x, 2);
		
		System.out.println("k" + k);
	}
}
