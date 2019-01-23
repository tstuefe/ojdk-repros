package de.stuefe.repros;

public class DeadLockExample implements Runnable {
	public int id = 1;
	static Object o1 = new Object();
	static Object o2 = new Object();
	public void run() {
		if (id == 0) {
			synchronized (o1) { 
				System.out.println("Thread " + id + ": locked o1");
				try {
					Thread.sleep(399);
				} catch (Exception e) {
				}
				synchronized (o2) {
					System.out.println("Thread " + id + ": locked o2");
				}
			}
		}
		if (id == 1) {
			synchronized (o2) {
				System.out.println("Thread " + id + ": locked o2");
				try {
					Thread.sleep(399);
				} catch (Exception e) {
				}
				synchronized (o1) {
					System.out.println("Thread " + id + ": locked o1");
				}
			}
		}
	}
	public static void main(String[] args) {
		DeadLockExample test0 = new DeadLockExample();
		DeadLockExample test1 = new DeadLockExample();
		test0.id = 0;
		test1.id = 1;
		Thread t1 = new Thread(test0);
		Thread t2 = new Thread(test1);
		t1.start();
		t2.start();
	}
}
