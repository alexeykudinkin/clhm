import concurrent.ConcurrentLinkedHashMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    private static final long READ_LATENCY_MS   = 25;
    private static final long WRITE_LATENCY_MS  = 50;
    private static final long REMOVE_LATENCY_MS = 75;

    private static final long REMOVE_OPS = 10000;


    private static final long READ_OPS = 1000000;
    private static final long WRITE_OPS = 100000;

    private static final int READERS = 32;
    private static final int WRITERS = 4;

    private static final int SIZE_THRESHOLD = 10000;


    public static void main(String[] args) {

        Map<String, Map> targets = new LinkedHashMap<>();

        targets.put(
                ConcurrentLinkedHashMap.class.getName(),
                new ConcurrentLinkedHashMap<Integer, Integer>(16, 0.75f, false) {
                    @Override
                    protected boolean removeEldestEntryForKey(Integer key) {
                        return size() > SIZE_THRESHOLD;
                    }
                }
        );

        targets.put(
            LinkedHashMap.class.getName(),
            Collections.synchronizedMap(
                new LinkedHashMap<Integer, Integer>(16, 0.75f, false) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, Integer> e) {
                        return size() > SIZE_THRESHOLD;
                    }
                }
            )
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

            LoadTestbed loader = new LoadTestbed(target.getValue(), WRITERS, READERS, WRITE_OPS, READ_OPS);

            loader.run();

            System.out.println("AVERAGE RDL: " + loader.getAverageReadingLatency() + " ns.");
            System.out.println("AVERAGE WRL: " + loader.getAverageWritingLatency() + " ns.");

        }

    }

}
