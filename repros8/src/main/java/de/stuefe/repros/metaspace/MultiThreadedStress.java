package de.stuefe.repros.metaspace;

import de.stuefe.repros.MiscUtils;
import de.stuefe.repros.metaspace.internals.InMemoryClassLoader;
import de.stuefe.repros.metaspace.internals.Utils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "MultiThreadedStress", mixinStandardHelpOptions = true,
        description = "MultiThreadedStress repro.")
public class MultiThreadedStress implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new MultiThreadedStress()).execute(args);
        System.exit(exitCode);
    }


    @CommandLine.Option(names = { "--profile", "-p" }, defaultValue = "M",
            description = "Test profile (XS, S, M, L, XL, XXL).")
    String profile_name;

    @CommandLine.Option(names = { "--time", "-t" }, defaultValue = "30",
            description = "Time this test should take, in seconds.")
    int time;

    static Random rand = new Random();

    static volatile boolean _doStop = false;
    static volatile boolean _gcNeeded = false;

    enum SizeSpec {
        XS(2),
        S(10),
        M(50),
        L(100),
        XL(400),
        XXL(2000);

        private final int avgSize;

        SizeSpec(int avgSize) {
            this.avgSize = avgSize;
        }

        public int getAvgSize() {
            return avgSize;
        }
    }

    ;

    static String nameClass(SizeSpec sizeSpec, int id) {
        return "myclass_" + sizeSpec.name() + "_" + id;
    }

    static int wobbleRandomlyAroundPivotPoint(int pivot) {
        int amount = Integer.max(pivot / 10, 1);
        return wobbleRandomlyAroundPivotPoint(pivot, amount);
    }

    static int wobbleRandomlyAroundPivotPoint(int pivot, int amount) {
        return wobbleRandomlyAroundPivotPoint(pivot, amount, 0, Integer.MAX_VALUE);
    }

    static int wobbleRandomlyAroundPivotPoint(int pivot, int amount, int min, int max) {
        int lowest = Integer.max(pivot - amount, min);
        int highest = Integer.min(pivot + amount, max - 1);
        int range = highest - lowest;
        int n = rand.nextInt(range) - (pivot - lowest);
        int result = pivot + n;
        if (result < min) {
            result = min;
        } else if (result >= max) {
            result = max - 1;
        }
        return result;
    }

    static void prepareClasses(int num, SizeSpec sizeSpec) {
        System.out.println("preparing " + num + "classes of size " + sizeSpec.name() + "...");
        for (int i = 0; i < num; i++) {
            int sizeFactor = wobbleRandomlyAroundPivotPoint(sizeSpec.getAvgSize());
            String name = nameClass(sizeSpec, i);
            Utils.createRandomClass(name, sizeFactor);
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
        }
    }

    static class TestClassLoader extends InMemoryClassLoader {
        final SizeSpec _sizeSpec;
        final int _lifeSpan;
        final int _maxClassesToLoad;
        int _classesLoaded;
        int _age;

        ArrayList<Class> _loadedClasses = new ArrayList<>();

        private void loadClassByNumber(int classId) {
            String name = nameClass(_sizeSpec, classId);
            Class<?> clazz = null;
            try {
                clazz = Class.forName(name, true, this);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            _loadedClasses.add(clazz);
        }

        private void maybeLoadAClass() {
            if (rand.nextInt(9) < 3 && _classesLoaded < _maxClassesToLoad) {
                loadClassByNumber(_classesLoaded);
                _classesLoaded++;
            }
        }

        public TestClassLoader(int lifeSpan, int maxClassesToLoad, SizeSpec sizeSpec) {
            _sizeSpec = sizeSpec;
            this._lifeSpan = lifeSpan;
            this._maxClassesToLoad = maxClassesToLoad;
            this._classesLoaded = this._age = 0;

        }

        boolean tick() {
            maybeLoadAClass();
            _age++;
            if (_age >= _lifeSpan) {
                // Please kill me.
                return true;
            }
            return false;
        }

    }

    ;

    static class TestThread extends Thread {

        final int _maxLoaders;
        final int _avgLoaderLifeSpawn;
        final int _maxClassesPerLoader;
        final int _avgTickTime;
        final SizeSpec _sizeSpec;

        LinkedList<TestClassLoader> _loaders = new LinkedList<>();

        boolean _upswing;

        public TestThread(int maxLoaders, int avgLoaderLifeSpawn, int maxClassesPerLoader, SizeSpec sizeSpec, int avgTickTime) {
            this._maxLoaders = maxLoaders;
            this._avgLoaderLifeSpawn = avgLoaderLifeSpawn;
            _maxClassesPerLoader = maxClassesPerLoader;
            _sizeSpec = sizeSpec;
            _avgTickTime = avgTickTime;
            _upswing = true;
        }

        private void tick() {
            int firstNull = -1;
            int loaders_created = 0;
            int loaders_removed = 0;

            // tick loaders and remove dead ones

            LinkedList<TestClassLoader> deadLoaders = new LinkedList<>();
            for (TestClassLoader ld : _loaders) {
                if (ld.tick()) {
                    deadLoaders.add(ld);
                }
            }

            loaders_removed = deadLoaders.size();
            _loaders.removeAll(deadLoaders);

            // on upswing, create a new loaders till max.
            // On downswing, wait until the loaders are drained to a small percentage to have a breath in breath out
            // effect.
            if (_upswing) {
                if (_loaders.size() < _maxLoaders) {
                    int lifeSpan = wobbleRandomlyAroundPivotPoint(_avgLoaderLifeSpawn);
                    TestClassLoader ld = new TestClassLoader(lifeSpan, _maxClassesPerLoader, _sizeSpec);
                    _loaders.add(ld);
                } else {
                    _upswing = false;
                }
            } else {
                if (_loaders.size() <= _maxLoaders / 10) {
                    _upswing = true;
                }
            }

            if (loaders_removed > 0) {
                _gcNeeded = true;
            }
        }

        @Override
        public void run() {
            while (!_doStop) {
                try {
                    if (_avgTickTime > 0) {
                        Thread.sleep(_avgTickTime / 2);
                    }
                    tick();
                    if (_avgTickTime > 0) {
                        Thread.sleep(_avgTickTime / 2);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    static class GCInstigatingThread extends Thread {
        final int _interval;
        int _gcCounter;

        GCInstigatingThread(int interval) {
            _interval = interval;
        }

        @Override
        public void run() {

            while (!_doStop) {
                try {
                    Thread.sleep(_interval);
                    if (_gcNeeded) {
                        System.gc();
                        _gcCounter++;

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            super.run();
        }
    }

    enum TestProfile {

        // default test profile
        XS(10, 10, 10, 1, 50, 1000, SizeSpec.XS),
        XS_NOGC(10, 10, 10, 1, -1, 1000, SizeSpec.XS),
        M(10, 10, 100, 1, 50, 1000, SizeSpec.M),
        M_NOGC(10, 10, 100, 1, -1, 1000, SizeSpec.M),
        ;

        final int numThreads;
        final int maxLoadersPerThread;
        final int maxClassesPerLoader;
        final int avgTickTime;
        final int gcFrequency; // in ms
        final int avgLoaderLifeSpan;
        final SizeSpec sizeSpec;

        TestProfile(int numThreads, int maxLoadersPerThread, int maxClassesPerLoader, int avgTickTime, int gcFrequency, int avgLoaderLifeSpan, SizeSpec sizeSpec) {
            this.numThreads = numThreads;
            this.maxLoadersPerThread = maxLoadersPerThread;
            this.maxClassesPerLoader = maxClassesPerLoader;
            this.avgTickTime = avgTickTime; // avg tick time in ms
            this.gcFrequency = gcFrequency; // avg span between artificially induced gc, in ms. -1 if none.
            this.avgLoaderLifeSpan = avgLoaderLifeSpan;
            this.sizeSpec = sizeSpec;
        }

        static TestProfile getForString(String name) {
            for (TestProfile p : TestProfile.values()) {
                if (name.equalsIgnoreCase(p.toString())) {
                    return p;
                }
            }
            return null;
        }

    }

    @Override
    public Integer call() throws Exception {

        System.out.println("Profile: " + profile_name + ".");
        System.out.println("Time: " + time + "s.");

        TestProfile profile = TestProfile.getForString(profile_name);
        if (profile == null) {
            throw new Exception("Invalid Profile name " + profile_name);
        }

        System.out.println("Time: " + time);

        prepareClasses(profile.maxClassesPerLoader, profile.sizeSpec);

        GCInstigatingThread gcThread = null;
        if (profile.gcFrequency != -1) {
            int freq = profile.gcFrequency;
            gcThread = new GCInstigatingThread(freq);
            gcThread.start();
        }

        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < profile.numThreads; i++) {
            TestThread t = new TestThread(profile.maxLoadersPerThread, profile.avgLoaderLifeSpan, profile.maxClassesPerLoader, profile.sizeSpec, profile.avgTickTime);
            threads.add(t);
        }

        for (Thread t : threads) {
            t.start();
        }

        if (time != -1) {
            System.out.println("time: " + time + "seconds...");
            try {
                Thread.sleep(time * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            MiscUtils.waitForKeyPress("press key to stop test");
        }

        _doStop = true;

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Num explicit gcs: " + gcThread._gcCounter);

        MiscUtils.waitForKeyPress("before final gc");
        System.gc();
        System.gc();
        MiscUtils.waitForKeyPress("after final gc");

        return 0;
    }



}
