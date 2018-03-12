package org.warps.pulsar.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.warps.pulsar.common.config.CapabilityTypes.GLOBAL_EXECUTOR_AUTO_CONCURRENCY_FACTOR;
import static org.warps.pulsar.common.config.CapabilityTypes.GLOBAL_EXECUTOR_CONCURRENCY_HINT;

public class GlobalExecutor implements AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(GlobalExecutor.class);
    /**
     * The number of processors available to the Java virtual machine
     */
    public static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static GlobalExecutor INSTANCE;
    private ExecutorService executor;
    private boolean closed = false;
    private float autoConcurrencyFactor;
    private int concurrency;

    private GlobalExecutor(ImmutableConfig immutableConfig) {
        int concurrencyHint = immutableConfig.getInt(GLOBAL_EXECUTOR_CONCURRENCY_HINT, -1);
        concurrency = concurrencyHint;
        if (concurrency <= 0) {
            autoConcurrencyFactor = immutableConfig.getFloat(GLOBAL_EXECUTOR_AUTO_CONCURRENCY_FACTOR, 1);
            concurrency = Math.max((int) (NCPU * autoConcurrencyFactor), 4);
        }

        Params.of(
                "availableProcessors", NCPU,
                "autoConcurrencyFactor", autoConcurrencyFactor,
                "concurrencyHint", concurrencyHint,
                "concurrency", concurrency
        ).withLogger(LOG).info(true);
    }

    public static GlobalExecutor getInstance(ImmutableConfig immutableConfig) {
        if (INSTANCE == null) {
            INSTANCE = new GlobalExecutor(immutableConfig);
        }

        return INSTANCE;
    }

    public int getConcurrency() {
        return concurrency;
    }

    /**
     * TODO: Allocate executors for sessions separately
     * TODO: Check
     */
    public ExecutorService getExecutor() {
        if (executor == null) {
            initExecutorService();
        }

        return executor;
    }

    private void initExecutorService() {
        synchronized (GlobalExecutor.class) {
            if (executor == null) {
                executor = Executors.newWorkStealingPool(concurrency);
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}
