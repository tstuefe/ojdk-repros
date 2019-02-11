package de.stuefe.repros.metaspace;

import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class MultiThreadStress {

    static Random rand = new Random();

    static String nameClass(int num) {
        return "myclass_" + num;
    }

    static void prepareClasses(int max) {
        System.out.println("prepping " + max + "classes...");
        for (int i = 0; i < max; i++) {
            int sizeFactor = rand.nextInt(98) + 2;
            String className = nameClass(i);
            Utils.createRandomClass(className, sizeFactor);
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
        }
        // Clean up compilation remnants
        System.gc(); System.gc();
        System.out.println("Done.");
    }



    static class TestClassLoader extends InMemoryClassLoader {
        final int _num;
        final int _lifeSpan;
        final int _maxClassesToLoad;
        int _classesLoaded;
        int _age;

        ArrayList<Class> _loadedClasses = new ArrayList<>();

        private void loadClassByNumber(ClassLoader ld, int num) {
            String name = nameClass(num);
            Class<?> clazz = null;
            try {
                clazz = Class.forName(name, true, ld);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            _loadedClasses.add(clazz);
        }

        private void maybeLoadAClass() {
            if (rand.nextInt(9) < 3 && _classesLoaded < _maxClassesToLoad) {
                loadClassByNumber(this, _classesLoaded);
                _classesLoaded ++;
            }
        }

        public TestClassLoader(int num, int lifeSpan, int maxClassesToLoad) {
            super("TestLoader" + num, null);
            this._num = num;
            this._lifeSpan = lifeSpan;
            this._maxClassesToLoad = maxClassesToLoad;
            this._classesLoaded = this._age = 0;
        }

        boolean tick() {
            maybeLoadAClass();
            _age++;
            if (_age >= _lifeSpan) {
                return true;
            }
            return false;
        }

    };

    static class TestThread extends Thread {

        final int _maxLoaders;
        final int _avgLifeSpawn;

        ClassLoader[] _loaders;

        public TestThread(int maxLoaders, int avgLifeSpawn) {
            this._maxLoaders = maxLoaders;
            this._avgLifeSpawn = avgLifeSpawn;
            

        }
    }




}
