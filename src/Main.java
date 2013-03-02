
import util.concurrent.ConcurrentHashMap;
import util.concurrent.ConcurrentLinkedHashMap;
import util.concurrent.ConcurrentLinkedHashMapV8;
import util.concurrent.jsr166e.ConcurrentHashMapV8;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    private static final int READ_OPS   = 100000;
    private static final int WRITE_OPS  = 100000;

    private static final int READERS    = 64;
    private static final int WRITERS    = 64;

    private static final int SIZE_THRESHOLD = 10000;


    public static void main(String[] args) {

        Map<String, Map> targets = new LinkedHashMap<>();

        //
        // Synchronized LinkedHashMap
        //

        targets.put(
                LinkedHashMap.class.getName(),
                Collections.synchronizedMap(
                        new LinkedHashMap<Integer, Integer>(16, 0.75f, false)
                )
        );

        //
        // OpenJDK ConcurrentHashMap
        //

        targets.put(
                ConcurrentHashMap.class.getCanonicalName(),
                new ConcurrentHashMap<Integer, Integer>(16, 0.75f, WRITERS)
        );

        //
        // J8 ConcurrentHashMap (by Doug Lea et al.)
        //

        targets.put(
                ConcurrentHashMapV8.class.getCanonicalName(),
                new ConcurrentHashMapV8<Integer, Integer>(16, 0.75f, WRITERS)
        );

        //
        // ConcurrentLinkedHashMap (based on OpenJDK CHM)
        //

        targets.put(
                ConcurrentLinkedHashMap.class.getCanonicalName(),
                new ConcurrentLinkedHashMap<Integer, Integer>(16, WRITERS, 0.75f, false)
        );

        //
        // ConcurrentLinkedHashMap (based on J8 CHM)
        //

        targets.put(
                ConcurrentLinkedHashMapV8.class.getCanonicalName(),
                new ConcurrentLinkedHashMapV8<Integer, Integer>(16, WRITERS, 0.75f, false)
        );

        //
        // Guava (?) ConcurrentLinkedHashMap
        //

        targets.put(
                com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.class.getName(),
                new com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder<Integer, Integer>()
                    .initialCapacity(16)
                    .concurrencyLevel(WRITERS)
                    .maximumWeightedCapacity(Long.MAX_VALUE)
                    .build()
        );


        //
        // LOAD TESTING
        //

        for (Map.Entry<String, Map> target : targets.entrySet()) {

            System.out.println("            ");
            System.out.println("============");
            System.out.println("TARGET #" + target.getKey());
            System.out.println("============");
            System.out.println("            ");

            CHMLoadTestbed ltb = new CHMLoadTestbed<Map<Integer, Integer>>(target.getValue(), WRITERS, READERS, WRITE_OPS, READ_OPS);

            ltb.warmRun();

            System.out.println("AVERAGE RDL: " + ltb.getAverageReadingLatency() + " ns.");
            System.out.println("AVERAGE WRL: " + ltb.getAverageWritingLatency() + " ns.");

        }

    }

}
