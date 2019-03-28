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
package ai.platon.pulsar.jobs.app.update;

import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.graph.GraphGroupKey;
import ai.platon.pulsar.persist.io.WebGraphWritable;

import java.util.Collection;

import static ai.platon.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION;
import static ai.platon.pulsar.common.config.PulsarConstants.JOB_CONTEXT_CONFIG_LOCATION;

public class InGraphUpdateJob extends WebGraphUpdateJob {

  public InGraphUpdateJob() {
  }

  @Override
  public void initJob() throws Exception {
    // Partition by {url}, sort by {url,score} and group by {url}.
    // This ensures that the inlinks are sorted by score when they enter the reducer.
    currentJob.setPartitionerClass(GraphGroupKey.UrlOnlyPartitioner.class);
    currentJob.setSortComparatorClass(GraphGroupKey.GraphKeyComparator.class);
    currentJob.setGroupingComparatorClass(GraphGroupKey.UrlOnlyComparator.class);

    Collection<GWebPage.Field> fields = getFields(currentJob);
    initMapper(currentJob, fields, GraphGroupKey.class, WebGraphWritable.class, InGraphUpdateMapper.class, getBatchIdFilter(batchId));
    initReducer(currentJob, InGraphUpdateReducer.class);
  }

  public static void main(String[] args) throws Exception {
    String configLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, JOB_CONTEXT_CONFIG_LOCATION);
    int res = run(configLocation, new InGraphUpdateJob(), args);
    System.exit(res);
  }
}
