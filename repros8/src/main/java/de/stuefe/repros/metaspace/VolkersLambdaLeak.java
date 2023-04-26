package de.stuefe.repros.metaspace;

import java.lang.invoke.*;
import java.lang.ref.WeakReference;

// Reproducer by Oli Gillespie / Amazon Corretto
//
// java11 -Xlog:unload*,nestmates*=debug -XX:MaxMetaspaceSize=10M -Xmx10M LambdaClassLeak
// ...
// [0,102s][info][class,unload] unloading class LambdaClassLeak$$Lambda$1/0x0000000100061040 0x0000000100061040
// Lambda class unloaded after 1 GCs
//
// java11 -XX:MaxMetaspaceSize=10M -Xmx10M LambdaClassLeak stress
// ...
// [0,696s][info][class,unload] unloading class LambdaClassLeak$$Lambda$8293/0x0000000100067840 0x0000000100067840
// END
//
// =====================================================================================
//
// java17 -Xlog:unload*,nestmates*=debug -XX:MaxMetaspaceSize=10M -Xmx10M LambdaClassLeak
// ...
// Lambda class NOT unloaded after 10 GCs
//
// java17 -XX:MaxMetaspaceSize=10M -Xmx10M LambdaClassLeak stress
// ...
// Exception in thread "main" java.lang.InternalError: java.lang.OutOfMemoryError: Metaspace
//
public class VolkersLambdaLeak {

    static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    static MethodHandle targetMethodHandle;

    public static void main(String[] args) throws Throwable {
        targetMethodHandle = LOOKUP.unreflect(Foo.class.getMethod("f", String.class));
        Foo fooBar = new FooBar();
        Foo bar = new Bar();

        CallSite cs = build();
        System.out.println("CallSite: " + cs);
        System.out.println("type=" + cs.type() + ", target=" + cs.getTarget() +
                " (" + cs.getTarget().getClass() + ")\n" +
                "target.invoke(fooBar).getClass()=" + cs.getTarget().invoke(fooBar).getClass());
        ((Foo)cs.getTarget().invoke(fooBar)).f("Hi");
        ((Foo)cs.getTarget().invoke(bar)).f("Hi");
        WeakReference<Class<?>> wcr = new WeakReference<>(cs.getTarget().invoke(fooBar).getClass());
        cs = null;
        int count = 0;
        while (wcr.get() != null && count++ < 10) {
            System.gc();
        }
        if (wcr.get() == null) {
            System.out.println("Lambda class unloaded after " + count + " GCs");
        } else {
            System.out.println("Lambda class NOT unloaded after " + (count - 1) + " GCs");
        }

        if (args.length > 0) {
            int max = Integer.parseInt(args[0]);
            for (int i = 0; i < max; i++) {
                if (i % 100_000 == 0) System.out.print('*');
                build();
            }
        }

        System.out.println("Keypress");
        System.in.read();

        System.out.println("END");
    }

    static public CallSite build() throws Throwable {
        return LambdaMetafactory.metafactory(LOOKUP,
                "f",
                MethodType.methodType(Foo.class, Foo.class),
                MethodType.methodType(void.class, String.class),
                targetMethodHandle,
                MethodType.methodType(void.class, String.class));
    }

    static interface Foo {
        default void f(String s) {
            System.out.println("Foo::f(" + s + ")");
        }
    }
    static class FooBar implements Foo {
    }
    static class Bar implements Foo {
        @Override
        public void f(String s) {
            System.out.println("Bar::f(" + s + ")");
        }
    }

}
