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
package ai.platon.pulsar.rest.model.response;

import ai.platon.pulsar.rest.model.request.JobConfig;
import ai.platon.pulsar.rest.service.deprecated.JobManager;
import com.google.common.collect.Maps;

import java.util.Map;

public class JobInfo {

  public enum State {
    IDLE, RUNNING, FINISHED, FAILED, KILLED, STOPPING, KILLING, ANY, NOT_FOUND, CAN_NOT_CREATE
  }

  private String id;
  private String jobName;
  private String crawlId;
  private int workerId;
  private JobManager.JobType type;
  private String confId;
  private Map<String, Object> args = Maps.newHashMap();
  private Map<String, Object> params = Maps.newHashMap();
  private Map<String, Object> status = Maps.newHashMap();
  private Map<String, Object> result = Maps.newHashMap();
  private State state;
  private long affectedRows = 0;
  private float progress = 0.0f;
  private String msg;

  public JobInfo(String id, JobConfig config, State state, String msg) {
    this.id = id;
    if (config != null) {
      this.crawlId = config.getCrawlId();
      this.type = config.getType();
      this.confId = config.getConfId();
      this.args = config.getArgs();
    }
    this.state = state;
    this.msg = msg;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getCrawlId() {
    return crawlId;
  }

  public void setCrawlId(String crawlId) {
    this.crawlId = crawlId;
  }

  public int getWorkerId() {
    return workerId;
  }

  public void setWorkerId(int workerId) {
    this.workerId = workerId;
  }

  public JobManager.JobType getType() {
    return type;
  }

  public void setType(JobManager.JobType type) {
    this.type = type;
  }

  public String getConfId() {
    return confId;
  }

  public void setConfId(String confId) {
    this.confId = confId;
  }

  public Map<String, Object> getArgs() {
    return args;
  }

  public void setArgs(Map<String, Object> args) {
    this.args = args;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  public Map<String, Object> getStatus() {
    return status;
  }

  public void setStatus(Map<String, Object> status) {
    this.status = status;
  }

  public Map<String, Object> getResult() {
    return result;
  }

  public void setResult(Map<String, Object> result) {
    this.result = result;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public long getAffectedRows() {
    return affectedRows;
  }

  public void setAffectedRows(long affectedRows) {
    this.affectedRows = affectedRows;
  }

  public float getProgress() {
    return progress;
  }

  public void setProgress(float progress) {
    this.progress = progress;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }
}
