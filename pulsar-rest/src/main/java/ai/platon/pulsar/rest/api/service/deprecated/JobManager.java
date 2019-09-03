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
package ai.platon.pulsar.rest.api.service.deprecated;

import ai.platon.pulsar.common.PulsarJobBase;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.rest.api.model.request.JobConfig;
import ai.platon.pulsar.rest.api.model.response.JobInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static ai.platon.pulsar.common.config.CapabilityTypes.STORAGE_CRAWL_ID;

public class JobManager {

    public enum JobType {
        INJECT,
        GENERATE,
        FETCH,
        PARSE,
        UPDATEDBOUT,
        UPDATEDBIN,
        READDB,
        PARSECHECKER,
        CLASS
    }

    public static final Logger LOG = LoggerFactory.getLogger(JobManager.class);

    private JobFactory jobFactory;
    private JobWorkerPoolExecutor executor;
    private JobConfigurations jobConfigurations;

    public JobManager(JobFactory jobFactory, JobWorkerPoolExecutor executor, JobConfigurations jobConfigurations) {
        this.jobFactory = jobFactory;
        this.executor = executor;
        this.jobConfigurations = jobConfigurations;
    }

    /**
     * Create a new Job if there is no other running jobs for the given JobConfig
     * We force only one job can be running under one FetchConfig,
     * and the FetchConfig is associated with an unique UI Crawl
     */
    public String create(JobConfig jobConfig) {
        if (jobConfig.getArgs() == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }

        // LOG.debug("Try to create job, job config : " + jobConfig.toString());

        // Do not create if there is already a running worker
        String configId = jobConfig.getConfId();
        JobWorker worker = executor.findRunningWorkerByConfig(configId);
        if (worker != null) {
            throw new IllegalStateException("Another running job using config : " + configId);
        }

        MutableConfig conf = cloneConfiguration(configId);

        if (jobConfig.getCrawlId() != null) {
            conf.set(STORAGE_CRAWL_ID, jobConfig.getCrawlId());
        }

        PulsarJobBase pulsarJob = createPulsarJob(jobConfig, conf.unbox());
        worker = new JobWorker(jobConfig, conf, pulsarJob);

        executor.execute(worker);
        executor.purge();

        return worker.getJobInfo().getId();
    }

    public Collection<JobInfo> list(String crawlId, JobInfo.State state) {
        if (state == null || state == JobInfo.State.ANY) {
            return executor.getAllJobs();
        }

        if (state == JobInfo.State.RUNNING || state == JobInfo.State.IDLE) {
            return executor.getRunningJobs();
        }

        return executor.getRetiredJobs();
    }

    public JobInfo get(String crawlId, String jobId) {
        JobWorker jobWorker = executor.findWorker(jobId);

        if (jobWorker != null) {
            return jobWorker.getJobInfo();
        }

        return new JobInfo(jobId, null, JobInfo.State.NOT_FOUND, "JOB NOT FOUND");
    }

    public JobWorker getJobWorker(String crawlId, String jobId) {
        return executor.findWorker(jobId);
    }

    private MutableConfig cloneConfiguration(String confId) {
        MutableConfig conf = jobConfigurations.get(confId);
        if (conf == null) {
            throw new IllegalArgumentException("Unknown confId " + confId);
        }

        return new MutableConfig(conf);
    }

    private PulsarJobBase createPulsarJob(JobConfig jobConfig, Configuration conf) {
        if (StringUtils.isNotBlank(jobConfig.getJobClassName())) {
            return jobFactory.createPulsarJobByClassName(jobConfig.getJobClassName(), conf);
        }

        return null;
    }

    public boolean abort(String crawlId, String jobId) {
        JobWorker jobWorker = executor.findWorker(jobId);
        if (jobWorker != null) {
            LOG.info("Kill PulsarConstants Job " + jobId);
            jobWorker.killJob();
            return true;
        }

        LOG.info("No such Job " + jobId);

        return false;
    }

    public boolean stop(String crawlId, String jobId) {
        JobWorker jobWorker = executor.findWorker(jobId);
        if (jobWorker != null) {
            LOG.info("Stop PulsarConstants Job " + jobId);
            jobWorker.stopJob();
            return true;
        }

        LOG.info("No such Job " + jobId);

        return false;
    }
}
