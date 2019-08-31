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
package ai.platon.pulsar.rest.service.deprecated;

import ai.platon.pulsar.common.PulsarJobBase;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.rest.model.request.JobConfig;
import ai.platon.pulsar.rest.model.response.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobWorker implements Runnable {

  public static final Logger LOG = LoggerFactory.getLogger(JobWorker.class);

  private PulsarJobBase pulsarJob;
  private JobInfo jobInfo;
  private JobConfig jobConfig;
  private AtomicBoolean running = new AtomicBoolean(false);

  public JobWorker(JobConfig jobConfig, MutableConfig conf, PulsarJobBase pulsarJob) {
    this.pulsarJob = pulsarJob;
    this.jobConfig = jobConfig;

    if (jobConfig.getConfId() == null) {
      jobConfig.setConfId(JobConfigurations.DEFAULT);
    }

    jobInfo = new JobInfo(generateJobId(jobConfig, hashCode()), jobConfig, JobInfo.State.IDLE, "idle");
  }

  @Override
  public void run() {
    try {
      running.set(true);

      jobInfo.setState(JobInfo.State.RUNNING);
      jobInfo.setMsg("OK");
      jobInfo.setResult(pulsarJob.run(Params.of(jobInfo.getArgs())));
      jobInfo.setState(JobInfo.State.FINISHED);
    } catch (Exception e) {
      LOG.error("Cannot run job worker!", e);
      jobInfo.setMsg("ERROR: " + e.toString());
      jobInfo.setState(JobInfo.State.FAILED);
    }
    finally {
      running.set(false);
    }
  }

  public boolean stopJob() {
    jobInfo.setState(JobInfo.State.STOPPING);

    try {
      return pulsarJob.stopJob();
    } catch (Exception e) {
      throw new RuntimeException("Cannot stop job with id " + jobInfo.getId(), e);
    }
  }

  public boolean killJob() {
    jobInfo.setState(JobInfo.State.KILLING);
    try {
      boolean result = pulsarJob.killJob();
      jobInfo.setState(JobInfo.State.KILLED);
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Cannot kill job with id " + jobInfo.getId(), e);
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  public String getJobId() {
    return jobInfo.getId();
  }

  public JobInfo getJobInfo() {
//    pulsarJob.updateStatus();
//
//    jobInfo.setJobName(pulsarJob.getJobName());
//    jobInfo.setAffectedRows(pulsarJob.getAffectedRows());
//    jobInfo.setProgress(pulsarJob.getProgress());
//    jobInfo.setParams(pulsarJob.getParams());
//    jobInfo.setCrawlStatus(pulsarJob.getBasicFields());
//    jobInfo.setProtocolOutput(pulsarJob.getResults());

    return jobInfo;
  }

  public String generateJobId(JobConfig jobConfig, int hashCode) {
    if (jobConfig.getCrawlId() == null) {
      return MessageFormat.format("{0}-{1}-{2}", jobConfig.getConfId(),
          jobConfig.getType(), String.valueOf(hashCode));
    }

    return MessageFormat.format("{0}-{1}-{2}-{3}", jobConfig.getCrawlId(),
        jobConfig.getConfId(), jobConfig.getType(), String.valueOf(hashCode));
  }
}
