import concurrent.ConcurrentHashMap;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: akudinkin
 * Date: 28.2.13
 */
public class LoadTestbed implements Runnable {

    LoadTestbed(Map target, int writers, int readers, long writingOperations, long readingOperations) {
        this.TARGET     = target;
        this.WRITERS    = writers;
        this.READERS    = readers;
        this.WR_OPS = writingOperations;
        this.RD_OPS = readingOperations;
    }

    @Override
    public void run() {

        // CONSECUTIVE

        final AtomicInteger WR_BARRIER = new AtomicInteger(0);

        Thread writers[] = new Thread[WRITERS];

        for (int i=0; i < WRITERS; ++i) {
            final int shift = i;
            writers[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (WR_BARRIER) {
                        try {

                            WR_BARRIER.incrementAndGet();
                            WR_BARRIER.wait();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }

                    // READY? STEADY! GO!

                    long start = System.nanoTime();

                    for (int i=0; i < WR_OPS; ++i) {
                        TARGET.put(i * shift, i);
                    }

                    long finish = System.nanoTime();

                    long avg = (finish - start) / WR_OPS;

                    WR_TIMESTAMPS.put(Thread.currentThread().getId(), avg);

                }
            });
            writers[i].start();
        }

        while (WR_BARRIER.get() < WRITERS) /* SPIN LOCK */;
        synchronized (WR_BARRIER) {
            WR_BARRIER.notifyAll();
        }

        for (int i=0; i < WRITERS; ++i) {
            try {
                writers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        final AtomicInteger RD_BARRIER = new AtomicInteger(0);

        Thread readers[] = new Thread[READERS];

        for (int i=0; i < READERS; ++i) {
            final int shift = i;
            readers[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (RD_BARRIER) {
                        try {

                            RD_BARRIER.incrementAndGet();
                            RD_BARRIER.wait();


                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // READY? STEADY! GO!

                    long start = System.nanoTime();

                    for (int i=0; i < RD_OPS; ++i) {
                        TARGET.get(i * shift);
                    }

                    long finish = System.nanoTime();

                    long avg = (finish - start) / RD_OPS;

                    RD_TIMESTAMPS.put(Thread.currentThread().getId(), avg);

                }
            });
            readers[i].start();
        }

        while (RD_BARRIER.get() < READERS) /* SPIN LOCK */;

        synchronized (RD_BARRIER) {
            RD_BARRIER.notifyAll();
        }

        for (int i=0; i < READERS; ++i) {
            try {
                readers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    protected long getAverageLatency(Map<Long, Long> source) {
        long avg = 0l;
        for (Map.Entry<Long, Long> e : source.entrySet()) {
            avg += e.getValue();
        }
        return avg / source.size();
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

    private final int   WRITERS;
    private final int   READERS;

    private final long  WR_OPS;
    private final long  RD_OPS;

    private final Map   TARGET;

}
