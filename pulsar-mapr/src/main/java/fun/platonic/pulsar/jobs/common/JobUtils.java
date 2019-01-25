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
package fun.platonic.pulsar.jobs.common;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static fun.platonic.pulsar.common.config.CapabilityTypes.STAT_PULSAR_STATUS;

public class JobUtils {

    public static final Logger LOG = LoggerFactory.getLogger(JobUtils.class);

    public static String generateBatchId() {
        return (System.currentTimeMillis() / 1000) + "-" + Math.abs(new Random().nextInt());
    }

    public static String generateConfigId() {
        return (System.currentTimeMillis() / 1000) + "-" + Math.abs(new Random().nextInt());
    }

    public static Map<String, Object> getJobStatus(Job job) {
        Map<String, Object> jobStates = new HashMap<>();
        if (job == null) {
            return jobStates;
        }

        Map<String, Object> runtimeStatus = getGroupedJobState(job, STAT_PULSAR_STATUS);
        jobStates.putAll(runtimeStatus);

        return jobStates;
    }

    public static Map<String, Object> getGroupedJobState(Job job, String... groups) {
        Map<String, Object> jobState = Maps.newTreeMap();
        if (job == null) {
            return jobState;
        }

        try {
            if (job.getStatus() == null || job.isRetired()) {
                return jobState;
            }
        } catch (IOException | InterruptedException e) {
            return jobState;
        }

        jobState.put("jobName", job.getJobName());
        jobState.put("jobID", job.getJobID());

        jobState.putAll(getJobCounters(job, groups));

        return jobState;
    }

    public static long getJobCounter(Job job, String groupName, String counterName) {
        try {
            return job.getCounters().getGroup(groupName).findCounter(counterName).getValue();
        } catch (Throwable e) {
            LOG.error("Failed find counter <" + groupName + ":" + counterName + ">");
        }
        return -1;
    }

    public static Map<String, Object> getJobCounters(Job job, String... groups) {
        Map<String, Object> counters = Maps.newTreeMap();
        if (job == null) {
            return counters;
        }

        try {
            for (CounterGroup group : job.getCounters()) {
                String groupName = group.getDisplayName();

                if (ArrayUtils.isEmpty(groups) || ArrayUtils.contains(groups, groupName)) {
                    Map<String, Object> groupedCounters = Maps.newTreeMap();

                    for (Counter counter : group) {
                        groupedCounters.put(counter.getName(), counter.getValue());
                    }

                    counters.put(groupName, groupedCounters);
                }
            }
        } catch (Exception e) {
            counters.put("error", e.toString());
        }

        return counters;
    }

    public long getCounterValue(Job job, String group, String name) throws IOException, InterruptedException {
        if (job == null || job.getStatus().isRetired()) {
            LOG.warn("Current job is null or job is retired");
            return 0;
        }

        if (job.getCounters() == null) {
            LOG.warn("No any counters");
            return 0;
        }

        Counter counter = job.getCounters().findCounter(group, name);
        if (counter == null) {
            LOG.warn("Can not find metricsCounters, group : " + group + ", name : " + name);

            return 0;
        }

        return counter.getValue();
    }

}
