package ai.platon.pulsar.skeleton.common;

import ai.platon.pulsar.common.config.ImmutableConfig;

import java.io.IOException;

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public interface ReducerContext<KEYIN, VALUEIN, KEYOUT, VALUEOUT> {
    ImmutableConfig getConfiguration();

    boolean nextKey() throws IOException, InterruptedException;

    boolean nextKeyValue() throws IOException, InterruptedException;

    KEYIN getCurrentKey() throws IOException, InterruptedException;

    VALUEIN getCurrentValue() throws IOException, InterruptedException;

    void write(KEYOUT var1, VALUEOUT var2) throws IOException, InterruptedException;

    Iterable<VALUEIN> getValues() throws IOException, InterruptedException;

    String getStatus();

    void setStatus(String var1);

    int getJobId();

    String getJobName();
}
