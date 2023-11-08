package de.stuefe.repros;

import java.time.ZoneId;

public class AllTimeZones {

        public static void main(String... args) {
            java.util.Set<String> list = ZoneId.getAvailableZoneIds();
            for (String s : list) {
                System.out.println(s);
            }
        }

}
