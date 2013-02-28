import concurrent.ConcurrentLinkedHashMap;

import java.util.*;

public class Main {

    private static final long READ_LATENCY_MS   = 25;
    private static final long WRITE_LATENCY_MS  = 50;
    private static final long REMOVE_LATENCY_MS = 75;

    private static final long REMOVE_OPS = 10000;


    private static final long READ_OPS = 100000000;
    private static final long WRITE_OPS = 10000000;

    private static final int READERS = 64;
    private static final int WRITERS = 16;

    public static void main(String[] args) {

        List<Map> targets = new ArrayList<Map>();

        targets.add(
            new ConcurrentLinkedHashMap<Integer, Integer>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntryForKey(Integer key) {
                    if (size() > 10000)
                        return true;
                    return false;
                }
            }
        );

        targets.add(
                Collections.synchronizedMap(
                    new LinkedHashMap<Integer, Integer>(16, 0.75f, false) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<Integer, Integer> e) {
                            if (size() > 10000)
                                return true;
                            return false;
                        }
                    }
                )
        );


        //
        // ...
        //

        for (int i=0; i < targets.size(); ++i) {

            System.out.println("            ");
            System.out.println("============");
            System.out.println("TARGET #" + i);
            System.out.println("============");
            System.out.println("            ");

            LoadHarness loader = new LoadHarness(targets.get(i), WRITERS, READERS, WRITE_OPS, READ_OPS);

            loader.run();

            System.out.println("AVERAGE RDL: " + loader.getAverageReadingLatency());
            System.out.println("AVERAGE WRL: " + loader.getAverageWritingLatency());

        }

    }

}
