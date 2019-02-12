package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.InMemoryJavaFileManager;
import de.stuefe.repros.metaspace.internals.Utils;

import java.util.LinkedList;

/***
 * 
 * Start the program with -XX:+UseCompressedClassPointers -XX:CompressedClassSpaceSize=10m.
 * 
 * Test loads a number of small classes, each in its own classloader. Then removes them and does
 * a GC. Then starts loading large classes until an OOM occurs.
 * 
 * @author Thomas Stuefe
 *
 */
public class BreatheInBreatheOut {

	private static String nameClass(int number, int sizeFactor) {
        return "myclass_size_" + sizeFactor + "_number_" + number;
    }

	private static void generateClasses(int numClasses, int sizeFactor) {
        for (int i = 0; i < numClasses; i++) {
            String className = nameClass(i, sizeFactor);
            Utils.createRandomClass(className, sizeFactor);
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
        }
    }

	public static void main(String args[]) throws Exception {

        int numSmallClasses = 3000;
        int sizeFactorSmallClasses = 1;
        int numLargeClasses = 300;
        int sizeFactorLargeClasses = 100;

        if (args.length > 0) {
            numSmallClasses = Integer.parseInt(args[0]);
            numLargeClasses = Integer.parseInt(args[1]);
        }

        System.out.print("Generate in memory class files...");
        System.out.print("Small (" + numSmallClasses + ") ...");
        generateClasses(numSmallClasses, sizeFactorSmallClasses);
        System.out.print("Done.");
        System.out.print("Large (" + numLargeClasses + ") ...");
        generateClasses(numLargeClasses, sizeFactorLargeClasses);
        System.out.println("Done.");

        System.gc();
        MiscUtils.waitForKeyPress();

        for (int run = 0; run < 1000; run ++) {

            LinkedList<ClassLoader> smallLoaders = new LinkedList<ClassLoader>();

            // load small classes
            System.out.print("Load " + numSmallClasses + " small classes into unique class loaders...");
			for (int i = 0; i < numSmallClasses; i++) {
				String className = nameClass(i, sizeFactorSmallClasses);
                InMemoryClassLoader aSmallLoader = new InMemoryClassLoader("small-loader", null);
                smallLoaders.add(aSmallLoader);
				Class<?> clazz = Class.forName(className, true, aSmallLoader);
                if (i % 100 == 0) {
                    System.out.println(i + "...");
                }
			}
			System.out.print("Done.");

            MiscUtils.waitForKeyPress("before GC");

			// clean all up
            System.out.print("GC...");
            smallLoaders.clear();
			System.gc();
            System.out.println("Done.");

            MiscUtils.waitForKeyPress();


            ///////////////////

            // Now create, inside a single classloader, load large classes.
            InMemoryClassLoader largeLoader = new InMemoryClassLoader("large-loader", null);
            System.out.print("Load " + numLargeClasses + " large classes into a single class loader...");
            for (int i = 0; i < numLargeClasses; i++) {
                String className = nameClass(i, sizeFactorLargeClasses);
                Class<?> clazz = Class.forName(className, true, largeLoader);
                if (i % 100 == 0) {
                    System.out.println(i + "...");
                }
            }
            System.out.print("Done.");

            MiscUtils.waitForKeyPress("before GC");

            System.out.print("GC...");
            largeLoader = null;
            System.gc();
            System.out.println("Done.");

            MiscUtils.waitForKeyPress();

		}
	}

}
