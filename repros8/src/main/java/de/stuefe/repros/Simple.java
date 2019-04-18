package de.stuefe.repros;

interface I1 {
    void say();
    void say2();
}

interface I2 {

}

abstract class SimpleBase implements I1, I2  {

}

public class Simple extends SimpleBase {

    int i = 0;
    Object o;
    Object o2;
    int i2 = 0;
    Object o3;
    Object o4;

    public void say() { System.out.print("hi"); }
    public void say2() { System.out.print("hi2"); }

    public static void main(String[] args) {

        Simple s = new Simple();
        Simple2 s2 = new Simple2();

        MiscUtils.waitForKeyPress();

    }



}


class Simple2 extends Simple {

}