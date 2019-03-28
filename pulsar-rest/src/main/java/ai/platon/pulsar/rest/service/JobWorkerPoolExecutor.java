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
package ai.platon.pulsar.rest.service;

import ai.platon.pulsar.rest.model.response.JobInfo;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JobWorkerPoolExecutor extends ThreadPoolExecutor {

  private Map<String, JobWorker> retiredWorkers = Maps.newHashMap();
  private Map<String, JobWorker> runningWorkers = Maps.newHashMap();

  public JobWorkerPoolExecutor(int corePoolSize, int maximumPoolSize,
      long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
  }

  public synchronized JobWorker findWorker(String jobId) {
    JobWorker worker = findRunningWorker(jobId);
    if (worker == null) {
      worker = findRetiredWorker(jobId);
    }

    return worker;
  }

  public synchronized JobWorker findRunningWorker(String jobId) {
    JobWorker worker = runningWorkers.get(jobId);

    if (worker != null && worker.isRunning()) {
      return worker;
    }

    return null;
  }

  public synchronized JobWorker findRunningWorkerByConfig(String configId) {
    for (JobWorker worker : runningWorkers.values()) {
      if (worker.isRunning()) {
        JobInfo jobInfo = worker.getJobInfo();
        if (jobInfo.getConfId().equals(configId)) {
          return worker;
        }
      }
    }

    return null;
  }

  public synchronized JobWorker findRetiredWorker(String jobId) {
    return retiredWorkers.get(jobId);
  }

  public synchronized int getRunningWorkersCount() {
    return runningWorkers.size();
  }

  public synchronized int getRetiredWorkersCount() {
    return retiredWorkers.size();
  }

  public synchronized Collection<JobInfo> getAllJobs() {
    return CollectionUtils.union(getRunningJobs(), getRetiredJobs());
  }

  public synchronized Collection<JobInfo> getRetiredJobs() {
    return getAllJobInfo(retiredWorkers);
  }

  public synchronized Collection<JobInfo> getRunningJobs() {
    return getAllJobInfo(runningWorkers);
  }

  public synchronized JobInfo getJobInfo(String jobId) {
    JobWorker jobWorker = findWorker(jobId);
    return jobWorker == null ? null : jobWorker.getJobInfo();
  }

  @Override
  protected void beforeExecute(Thread thread, Runnable runnable) {
    super.beforeExecute(thread, runnable);

    JobWorker worker = ((JobWorker) runnable);
    runningWorkers.put(worker.getJobId(), worker);
  }

  @Override
  protected void afterExecute(Runnable runnable, Throwable throwable) {
    super.afterExecute(runnable, throwable);

    JobWorker worker = ((JobWorker) runnable);
    runningWorkers.remove(worker.getJobId());
    retiredWorkers.put(worker.getJobId(), worker);
  }

  private Collection<JobInfo> getAllJobInfo(Map<String, JobWorker> workers) {
    return CollectionUtils.collect(workers.values(), new Transformer<JobWorker, JobInfo>() {
      @Override
      public JobInfo transform(JobWorker jobWorker) {
        return jobWorker.getJobInfo();
      }
    });
  }
}
