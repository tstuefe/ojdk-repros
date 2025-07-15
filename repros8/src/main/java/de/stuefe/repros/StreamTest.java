package de.stuefe.repros;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamTest {

    public void main(String[] args) {

        Set<String> switches =
            Arrays.stream(args).filter(a -> a.startsWith("--"))
                    .collect(Collectors.toSet());
        switches.stream().forEach(System.out::println);
                
    }

}
