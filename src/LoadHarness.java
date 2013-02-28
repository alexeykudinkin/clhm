import concurrent.ConcurrentHashMap;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: akudinkin
 * Date: 28.2.13
 */
public class LoadHarness implements Runnable {

    LoadHarness(Map target, int writers, int readers, long writingOperations, long readingOperations) {
        this.TARGET     = target;
        this.WRITERS    = writers;
        this.READERS    = readers;
        this.WRITE_OPS  = writingOperations;
        this.READ_OPS   = readingOperations;
    }

    @Override
    public void run() {

        // CONSECUTIVE

        final AtomicInteger wbarrier = new AtomicInteger(0);

        Thread writers[] = new Thread[WRITERS];

        System.out.println("============");
        System.out.println("WRITING     ");
        System.out.println("============");

        for (int i=0; i < WRITERS; ++i) {
            final int shift = i;
            writers[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (wbarrier) {
                        try {

                            wbarrier.incrementAndGet();
                            wbarrier.wait();

                            // READY? STEADY! GO!

                            long start = System.nanoTime();

                            for (int i=0; i < WRITE_OPS / WRITERS; ++i) {
                                TARGET.put(i * shift, i);
                            }

                            long finish = System.nanoTime();

                            long avg = (finish - start) / (WRITE_OPS / WRITERS);

                            System.out.println( "Thread #" + Thread.currentThread().getId() + ": " + avg + " ms.," +
                                    "for " + WRITE_OPS / WRITERS);

                            WRITE_TIMESTAMPS.put(Thread.currentThread().getName(), avg);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            writers[i].start();
        }

        while (wbarrier.get() < WRITERS) { /* SPIN LOCK */ }
        synchronized (wbarrier) {
            wbarrier.notifyAll();
        }

        for (int i=0; i < WRITERS; ++i) {
            try {
                writers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        final AtomicInteger rbarrier = new AtomicInteger(0);

        Thread readers[] = new Thread[READERS];

        System.out.println("============");
        System.out.println("READING     ");
        System.out.println("============");

        for (int i=0; i < READERS; ++i) {
            final int shift = i;
            readers[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (rbarrier) {
                        try {

                            rbarrier.incrementAndGet();
                            rbarrier.wait();

                            // READY? STEADY! GO!

                            long start = System.nanoTime();

                            for (int i=0; i < READ_OPS / READERS; ++i) {
                                TARGET.get(i * shift);
                            }

                            long finish = System.nanoTime();

                            long avg = (finish - start) / (READ_OPS / READERS);

                            System.out.println( "Thread #" + Thread.currentThread().getId() + ": " + avg + " ms., " +
                                    "for " + READ_OPS / READERS);

                            READ_TIMESTAMPS.put(Thread.currentThread().getName(), avg);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            readers[i].start();
        }

        while (rbarrier.get() < READERS) { /* SPIN LOCK */ }
        synchronized (rbarrier) {
            rbarrier.notifyAll();
        }

        for (int i=0; i < READERS; ++i) {
            try {
                readers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    protected long getAverageLatency(Map<String, Long> source) {
        long avg = 0l;
        for (Map.Entry<String, Long> e : source.entrySet()) {
            avg += e.getValue();
        }
        return avg / source.size();
    }


    public long getAverageReadingLatency() {
        return getAverageLatency(READ_TIMESTAMPS);
    }

    public long getAverageWritingLatency() {
        return getAverageLatency(WRITE_TIMESTAMPS);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private final ConcurrentHashMap<String, Long> READ_TIMESTAMPS   = new ConcurrentHashMap<String, Long>();
    private final ConcurrentHashMap<String, Long> WRITE_TIMESTAMPS  = new ConcurrentHashMap<String, Long>();

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private final int WRITERS;
    private final int READERS;
    private final long WRITE_OPS;
    private final long READ_OPS;

    private final Map TARGET;

}
