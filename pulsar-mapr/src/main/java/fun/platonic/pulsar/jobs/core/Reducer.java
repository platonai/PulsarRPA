/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package fun.platonic.pulsar.jobs.core;

import fun.platonic.pulsar.common.DateTimeUtil;
import fun.platonic.pulsar.common.MetricsCounters;
import fun.platonic.pulsar.common.MetricsReporter;
import fun.platonic.pulsar.common.config.Configurable;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

public class Reducer<K1, V1, K2, V2> extends org.apache.hadoop.mapreduce.Reducer<K1, V1, K2, V2> implements Configurable {

    protected static final Logger LOG = LoggerFactory.getLogger(Reducer.class.getName());

    protected Context context;

    protected ImmutableConfig conf;
    protected MetricsCounters metricsCounters;
    protected MetricsReporter pulsarReporter;

    protected boolean completed = false;

    protected Instant startTime = Instant.now();

    protected void beforeSetup(Context context) throws IOException, InterruptedException {
        this.context = context;
        this.conf = new ImmutableConfig(context.getConfiguration());

        this.metricsCounters = new MetricsCounters();
        this.pulsarReporter = new MetricsReporter(context.getJobName(), metricsCounters, conf, context);

        LOG.info(Params.formatAsLine(
                "---- reducer setup ", " ----",
                "className", this.getClass().getSimpleName(),
                "startTime", DateTimeUtil.format(startTime),
                "reducerTasks", context.getNumReduceTasks(),
                "hostname", metricsCounters.getHostname()
        ));
    }

    @Override
    public void run(Context context) {
        try {
            beforeSetup(context);
            setup(context);

            doRun(context);

            cleanup(context);

            cleanupContext(context);
        } catch (Throwable e) {
            LOG.error(StringUtils.stringifyException(e));
        } finally {
            afterCleanup(context);
        }
    }

    protected void doRun(Context context) throws IOException, InterruptedException {
        while (!completed && context.nextKey()) {
            reduce(context.getCurrentKey(), context.getValues(), context);
        }
    }

    protected void cleanupContext(Context context) throws Exception {
    }

    protected void afterCleanup(Context context) {
        context.setStatus(metricsCounters.getStatus(true));
        pulsarReporter.stopReporter();

        LOG.info(Params.formatAsLine(
                "---- reducer cleanup ", " ----",
                "className", this.getClass().getSimpleName(),
                "startTime", DateTimeUtil.format(startTime),
                "finishTime", DateTimeUtil.now(),
                "timeElapsed", DateTimeUtil.elapsedTime(startTime)
        ));
    }

    protected boolean completed() {
        return completed;
    }

    protected void abort() {
        completed = true;
    }

    protected void abort(String error) {
        LOG.error(error);
        completed = true;
    }

    protected void stop() {
        completed = true;
    }

    protected void stop(String info) {
        LOG.info(info);
        completed = true;
    }

    protected MetricsCounters getPulsarCounters() {
        return metricsCounters;
    }

    protected MetricsReporter getPulsarReporter() {
        return pulsarReporter;
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public void setConf(ImmutableConfig conf) {
        this.conf = conf;
    }
}
