package fun.platonic.pulsar.jobs.core;

import fun.platonic.pulsar.common.ReducerContext;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * Created by vincent on 16-9-24.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
 */
public class HadoopReducerContext<KEYIN, VALUEIN, KEYOUT, VALUEOUT> implements ReducerContext<KEYIN, VALUEIN, KEYOUT, VALUEOUT> {

    private Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT>.Context context;

    public HadoopReducerContext() {
    }

    public HadoopReducerContext(Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT>.Context context) {
        this.context = context;
    }

    public void setContext(Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT>.Context context) {
        this.context = context;
    }

    @Override
    public ImmutableConfig getConfiguration() {
        // Wrap configuration
        return new ImmutableConfig(context.getConfiguration());
    }

    @Override
    public boolean nextKey() throws IOException, InterruptedException {
        return context.nextKey();
    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        return context.nextKeyValue();
    }

    @Override
    public KEYIN getCurrentKey() throws IOException, InterruptedException {
        return context.getCurrentKey();
    }

    @Override
    public VALUEIN getCurrentValue() throws IOException, InterruptedException {
        return context.getCurrentValue();
    }

    @Override
    public void write(KEYOUT var1, VALUEOUT var2) throws IOException, InterruptedException {
        context.write(var1, var2);
    }

    @Override
    public Iterable<VALUEIN> getValues() throws IOException, InterruptedException {
        return context.getValues();
    }

    @Override
    public String getStatus() {
        return context.getStatus();
    }

    @Override
    public void setStatus(String var1) {
        context.setStatus(var1);
    }

    @Override
    public int getJobId() {
        return context.getJobID().getId();
    }

    @Override
    public String getJobName() {
        return context.getJobName();
    }
}
