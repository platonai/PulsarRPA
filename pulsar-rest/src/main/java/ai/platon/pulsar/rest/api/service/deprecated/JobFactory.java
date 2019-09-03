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
import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;

import java.util.Map;

public class JobFactory {
  private static Map<JobManager.JobType, String> typeToClass;

  static {
    typeToClass = Maps.newHashMap();

    typeToClass.put(JobManager.JobType.INJECT, "ai.platon.pulsar.jobs.inject.InjectJob");
    typeToClass.put(JobManager.JobType.FETCH, "ai.platon.pulsar.jobs.fetch.FetchJob");
    typeToClass.put(JobManager.JobType.GENERATE, "ai.platon.pulsar.jobs.generate.GenerateJob");
    typeToClass.put(JobManager.JobType.PARSE, "ai.platon.pulsar.jobs.parse.ParserJob");
    typeToClass.put(JobManager.JobType.UPDATEDBOUT, "ai.platon.pulsar.jobs.update.OutGraphUpdateJob");
    typeToClass.put(JobManager.JobType.UPDATEDBIN, "ai.platon.pulsar.jobs.update.InGraphUpdateJob");
    typeToClass.put(JobManager.JobType.PARSECHECKER, "ai.platon.pulsar.jobs.parse.ParserCheckJob");
  }

  public PulsarJobBase createPulsarJobByClassName(String className, Configuration conf) {
    try {
      Class clz = Class.forName(className);
      return createPulsarJob(clz, conf);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private PulsarJobBase createPulsarJob(Class<? extends PulsarJobBase> clz, Configuration conf) {
    return ReflectionUtils.newInstance(clz, conf);
  }
}
