package de.stuefe.repros;


import de.stuefe.repros.metaspace.ManyLambdas;
import picocli.CommandLine;

public class SimpleSleep {

    public static void main(String[] args) throws Exception{
        Thread.sleep(3600000);
    }

}
