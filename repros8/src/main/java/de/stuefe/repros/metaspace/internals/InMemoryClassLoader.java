package de.stuefe.repros.metaspace.internals;

public class InMemoryClassLoader extends ClassLoader {

    public InMemoryClassLoader(String name, ClassLoader parent) {
        // 1.9 ++ but we want to keep 1.8 compatibility
        // super(name, parent);
        super(parent);
    }

    public InMemoryClassLoader() {
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        InMemoryClassFile o = InMemoryJavaFileManager.theFileManager().getClassFile(name, false);
        if (o != null) {
            byte[] bytes = o.getBytes();
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }
}

