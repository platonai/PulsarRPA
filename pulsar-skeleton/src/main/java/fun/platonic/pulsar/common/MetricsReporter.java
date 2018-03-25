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
package fun.platonic.pulsar.common;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static fun.platonic.pulsar.common.config.CapabilityTypes.REPORTER_REPORT_INTERVAL;

/**
 * TODO : Use org.apache.hadoop.metrics2
 */
public class MetricsReporter extends Thread {

    public static final Logger LOG_ADDITIVITY = LoggerFactory.getLogger(MetricsReporter.class + "Add");
    public static final Logger LOG_NON_ADDITIVITY = LoggerFactory.getLogger(MetricsReporter.class);
    protected final ImmutableConfig conf;
    private final MetricsCounters counter;
    private final String jobName;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean silent = new AtomicBoolean(false);
    private Logger LOG = LOG_NON_ADDITIVITY;
    private TaskInputOutputContext context;
    private Duration reportInterval;

    public MetricsReporter(String jobName, MetricsCounters counter, ImmutableConfig conf, TaskInputOutputContext context) {
        this.conf = conf;
        this.context = context;
        this.jobName = jobName;
        this.counter = counter;
        this.reportInterval = conf.getDuration(REPORTER_REPORT_INTERVAL, Duration.ofSeconds(10));

        final String name = "Reporter-" + counter.id();

        setName(name);
        setDaemon(true);

        startReporter();
    }

    public Logger getLog() {
        return LOG;
    }

    public void setLog(Logger log) {
        LOG = log;
    }

    /**
     * Set report interval in seconds
     *
     * @param interval report interval in second
     */
    public void setReportInterval(Duration interval) {
        this.reportInterval = interval;
    }

    public void silence() {
        this.silent.set(true);
    }

    public void startReporter() {
        if (!running.get()) {
            start();
            running.set(true);
        }
    }

    public void stopReporter() {
        running.set(false);

        silent.set(false);

        try {
            join();
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
    }

    @Override
    public void run() {
        String outerBorder = StringUtils.repeat('-', 100);
        String innerBorder = StringUtils.repeat('.', 100);
        LOG.info(outerBorder);
        LOG.info(innerBorder);
        LOG.info("== Reporter started [ " + DateTimeUtil.now() + " ] [ " + jobName + " ] ==");
        LOG.debug("All registered counters : " + counter.getRegisteredCounters());

        do {
            try {
                sleep(reportInterval.toMillis());
            } catch (InterruptedException ignored) {
            }

            counter.accumulateGlobalCounters(context);
            report();
        }
        while (running.get());

        LOG.info("== Reporter stopped [ " + DateTimeUtil.now() + " ] ==");
    } // run

    private void report() {
        // Can only access variables in this thread
        if (!silent.get()) {
            String status = counter.getStatus(false);
            if (!status.isEmpty()) {
                LOG.info(status);
            }
        }
    }
}
