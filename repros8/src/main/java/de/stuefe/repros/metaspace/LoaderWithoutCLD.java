package de.stuefe.repros.metaspace;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;

import java.lang.reflect.*;
import java.io.*;

// https://bugs.openjdk.org/browse/JDK-8293156

public class LoaderWithoutCLD {

    static class Helper {
        public static void foo() {
            System.out.println("Helper foo");
        }
    }

    static class DelegatingLoader extends ClassLoader {
        DelegatingLoader(ClassLoader parent) {
            super(parent);
        }
    }

    public static void main(String[] args) throws Throwable {
        Utils.createRandomClass("SomeClass", 10);
        ClassLoader ccl = LoaderWithoutCLD.class.getClassLoader();
        ClassLoader l1 = new DelegatingLoader(ccl);
        ClassLoader l2 = new DelegatingLoader(l1);
        ClassLoader l3 = new DelegatingLoader(l2);
        ClassLoader l4 = new InMemoryClassLoader("XXX", l3);

        ClassLoader l5 = new DelegatingLoader(ccl);
        ClassLoader l6 = new DelegatingLoader(l5);
        ClassLoader l7 = new DelegatingLoader(l6);
        // These should be folded
        ClassLoader l8 = new InMemoryClassLoader("XXX2", l7);
        ClassLoader l9 = new InMemoryClassLoader("XXX2", l7);
        ClassLoader l10 = new InMemoryClassLoader("XXX2", l7);

         //   Class.forName("de.stuefe.repros.metaspace.LoaderWithoutCLD$Helper", true, l1);

        Class.forName("SomeClass", true, l4);
        Class.forName("SomeClass", true, l8);
        Class.forName("SomeClass", true, l9);
        Class.forName("SomeClass", true, l10);

       /*     Method m = c.getDeclaredMethod("foo", new Class[] { });
            m.invoke(null, new Object[] {}); */
            System.out.println("press key");
            System.in.read();
        }

}
