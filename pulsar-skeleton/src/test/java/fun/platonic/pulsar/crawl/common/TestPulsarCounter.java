package fun.platonic.pulsar.crawl.common;

import fun.platonic.pulsar.common.MetricsCounters;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
 */
public class TestPulsarCounter {

    static {
        MetricsCounters.register(Counter1.class);
        MetricsCounters.register(Counter2.class);
        MetricsCounters.register(Counter3.class);
        // It's OK to re-register
        MetricsCounters.register(Counter3.class);
        MetricsCounters.register(Counter3.class);
    }

    @Test
    public void testPulsarCounter() {
        MetricsCounters counter = new MetricsCounters();
        int group1 = MetricsCounters.getGroup(Counter1.class);
        int group2 = MetricsCounters.getGroup(Counter2.class);
        int group3 = MetricsCounters.getGroup(Counter3.class);
        int group4 = MetricsCounters.getGroup(Counter3.class);

        assertEquals(1, group1);
        assertEquals(2, group2);
        assertEquals(3, group3);
        assertEquals(3, group4);

        assertEquals(3, MetricsCounters.getGroup(Counter3.class));
        assertEquals(3, MetricsCounters.getGroup(Counter3.stFetched));

        counter.increase(Counter1.rDepth0);
        counter.increase(Counter2.inlinks);
        counter.increase(Counter2.rDepthN);
        counter.increase(Counter2.rDepth3);

        assertEquals(1, counter.get(Counter1.rDepth0));
        assertEquals(1, counter.get(Counter2.rDepth3));

        counter.increase(Counter2.rDepth1);
        counter.increase(Counter2.rDepth1);
        counter.increase(Counter2.rDepth1);
        counter.increase(Counter2.rDepth1);
        counter.increase(Counter2.rDepth1);
        counter.increase(Counter2.rDepth1);
        assertEquals(6, counter.get(Counter2.rDepth1));

        counter.setValue(Counter2.rDepth1, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, counter.get(Counter2.rDepth1));

        counter.increase(group3, Counter3.liveLinks);
        assertEquals(1, counter.get(Counter3.liveLinks));
        counter.increase(Counter3.stFetched);
        counter.increase(Counter3.stGone);

        assertEquals(1, counter.get(Counter3.stGone));

        assertEquals("1'rDepth0:1, 2'rDepth1:2147483647, 2'rDepth3:1, 2'rDepthN:1, 2'inlinks:1, 3'liveLinks:1, 3'stFetched:1, 3'stGone:1", counter.getStatus(true));
        assertEquals("rDepth0:1, rDepth1:2147483647, rDepth3:1, rDepthN:1, inlinks:1, liveLinks:1, stFetched:1, stGone:1", counter.getStatus(false));
    }

    public void operationOnCounter(MetricsCounters counter, int group3) {
        counter.increase(Counter1.rDepth1);
        counter.increase(Counter3.rDepth1);

        counter.increase(Counter2.rDepth2);
        counter.increase(Counter2.rDepth3);

        counter.get(Counter1.rDepth1);
        counter.get(Counter3.stFetched);

        counter.increase(Counter2.rDepth2, -1);
        counter.increase(Counter2.rDepth3, -1);

        counter.setValue(group3, Counter3.rDepth1, 0);
    }

    @Test
    @Ignore
    public void testPulsarCounterMultipleThreaded() {
        MetricsCounters counter = new MetricsCounters();
        int group3 = MetricsCounters.getGroup(Counter3.class);

        final int maxThreads = 2000;
        int countDown = maxThreads;
        ExecutorService taskExecutor = Executors.newFixedThreadPool(maxThreads);
        while (countDown-- > 0) {
            taskExecutor.execute(() -> {
                operationOnCounter(counter, group3);
//        try {
//          Thread.sleep(100);
//        }
//        catch (InterruptedException ignored) {}
            });
        }
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }

        assertEquals(0, counter.get(Counter2.rDepth2));
        assertEquals(0, counter.get(Counter2.rDepth3));

        assertEquals(maxThreads, counter.get(Counter1.rDepth1));
        assertEquals(0, counter.get(Counter3.rDepth1));
    }

    private enum Counter1 {rDepth0, rDepth1, rDepth2, rDepth3, rDepthN}

    private enum Counter2 {rDepth0, rDepth1, rDepth2, rDepth3, rDepthN, inlinks, liveLinks}

    private enum Counter3 {
        rDepth0, rDepth1, rDepth2, rDepth3, rDepthN, inlinks, liveLinks,
        stFetched, stRedirTemp, stRedirPerm, stNotModified, stRetry, stUnfetched, stGone
    }
}
