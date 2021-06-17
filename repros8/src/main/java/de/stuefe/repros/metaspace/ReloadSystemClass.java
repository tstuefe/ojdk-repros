package de.stuefe.repros.metaspace;

import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "InterleavedLoaders", mixinStandardHelpOptions = true,
        description = "InterleavedLoaders repro.")
public class ReloadSystemClass implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new ReloadSystemClass()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Option(names = {"--num"}, defaultValue = "5",
            description = "Number of reloads.")
    int num;

    public class MyClassLoader extends ClassLoader {

        @Override
        public Class findClass(String name) throws ClassNotFoundException {
            byte[] b = loadClassFromFile(name);
            return defineClass(name, b, 0, b.length);
        }

        private byte[] loadClassFromFile(String name)  {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
                    name.replace('.', File.separatorChar) + ".class");
            byte[] buffer;
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            int nextValue = 0;
            try {
                while ( (nextValue = inputStream.read()) != -1 ) {
                    byteStream.write(nextValue);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            buffer = byteStream.toByteArray();
            return buffer;
        }
    }

    @Override
    public Integer call() throws Exception {

        MyClassLoader cl = new MyClassLoader();
        Class c = cl.findClass("java.util.Pattern");
        Utils.waitForKeyPress(null);
        return 0;
    }

}