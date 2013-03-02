import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class CHMLoadTestbed<Map extends java.util.Map> {

    private final double WARMING_OPS_RATIO = .2;


    CHMLoadTestbed(Map target, int writers, int readers, int writingOperations, int readingOperations) {
        this.target = target;
        this.writers = writers;
        this.readers = readers;
        this.pureWritingOperations = writingOperations;
        this.pureReadingOperations = readingOperations;
    }


    /**
     * Micro-benchmarking procedure preceded by the warming round entailing consecutive series of reading/writing
     * operations to properly prepare contestants and exclude burden entailed by the class-loading/initialization etc.
     */
    public void warmRun() {

        final AtomicInteger WR_BARRIER = new AtomicInteger(0);

        Thread writers[] = new Thread[this.writers];


        //
        // WRITING LOAD
        //

        for (int i=0; i < this.writers; ++i) {
            final int id = i;
            writers[i] = new Thread(new Runnable() {
                @Override
                public void run() {

                    Thread.currentThread().setName("WR_" + target.getClass().getName() + "_" + id);

                    Random seed = new Random();

                    //
                    // WARMING ROUND
                    //

                    for (int i=0; i < pureWritingOperations * WARMING_OPS_RATIO; ++i) {
                        target.put(seed.nextInt() % (pureWritingOperations * CHMLoadTestbed.this.writers), i);
                    }

                    // EXPLICIT BARRIER

                    synchronized (WR_BARRIER) {
                        try {
                            WR_BARRIER.incrementAndGet();
                            WR_BARRIER.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // READY? STEADY! GO!

                    long acc = 0l;

                    for (int i=0; i < pureWritingOperations; ++i) {

                        long start = System.nanoTime();

                        target.put(seed.nextInt() % (pureWritingOperations * CHMLoadTestbed.this.writers), i);

                        long finish = System.nanoTime();

                        acc += finish - start;

                    }

                    WR_TIMESTAMPS.put(Thread.currentThread().getId(), acc / pureWritingOperations);

                }
            });
            writers[i].start();
        }

        // Wait until the target would get "warmed up"

        while (WR_BARRIER.get() < this.writers) /* SPIN LOCK */;

        synchronized (WR_BARRIER) {

            target.clear();

            WR_BARRIER.notifyAll();

        }

        for (int i=0; i < this.writers; ++i) {
            try {
                writers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        //
        // READING LOAD
        //

        final AtomicInteger RD_BARRIER = new AtomicInteger(0);

        Thread readers[] = new Thread[this.readers];

        for (int i=0; i < this.readers; ++i) {
            final int seq = i;
            readers[i] = new Thread(new Runnable() {
                @Override
                public void run() {

                    Thread.currentThread().setName("RD_" + target.getClass().getName() + "_" + seq);

                    Random seed = new Random();

                    //
                    // WARMING ROUND
                    //

                    for (int i=0; i < pureWritingOperations / WARMING_OPS_RATIO; ++i) {
                        target.get(seed.nextInt() % (pureWritingOperations * CHMLoadTestbed.this.writers));
                    }

                    // EXPLICIT BARRIER

                    synchronized (RD_BARRIER) {
                        try {
                            RD_BARRIER.incrementAndGet();
                            RD_BARRIER.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // READY? STEADY! GO!

                    long acc = 0l;

                    for (int i=0; i < pureReadingOperations; ++i) {

                        long start = System.nanoTime();

                        target.get(seed.nextInt() % (pureWritingOperations * CHMLoadTestbed.this.writers));

                        long finish = System.nanoTime();

                        acc += finish - start;

                    }

                    RD_TIMESTAMPS.put(Thread.currentThread().getId(), acc / pureReadingOperations);
                }
            });
            readers[i].start();
        }

        while (RD_BARRIER.get() < this.readers) /* SPIN LOCK */;

        synchronized (RD_BARRIER) {
            RD_BARRIER.notifyAll();
        }

        for (int i=0; i < this.readers; ++i) {
            try {
                readers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        target.clear();

    }


    protected long getAverageLatency(java.util.Map<Long, Long> source) {
        long avg = 0l;
        for (Map.Entry<Long, Long> e : source.entrySet()) {
            avg += e.getValue();
        }
        return avg / source.size();
    }


    protected void dumpLatencies(java.util.Map<Long, Long> source) {
        for (Map.Entry<Long, Long> e : source.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue());
        }
    }


    public void dumpReadingLatencies() {
        assert RD_TIMESTAMPS.size() == readers;
        dumpLatencies(RD_TIMESTAMPS);
    }

    public void dumpWritingLatencies() {
        assert WR_TIMESTAMPS.size() == writers;
        dumpLatencies(WR_TIMESTAMPS);
    }

    public long getAverageReadingLatency() {
        return getAverageLatency(RD_TIMESTAMPS);
    }

    public long getAverageWritingLatency() {
        return getAverageLatency(WR_TIMESTAMPS);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private final ConcurrentHashMap<Long, Long> RD_TIMESTAMPS = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> WR_TIMESTAMPS = new ConcurrentHashMap<>();

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private final int writers;
    private final int readers;

    private final int pureWritingOperations;
    private final int pureReadingOperations;

    private final Map target;

}
