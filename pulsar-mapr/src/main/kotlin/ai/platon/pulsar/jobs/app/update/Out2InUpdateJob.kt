/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.jobs.app.update

import ai.platon.pulsar.jobs.JobEnv
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import ai.platon.pulsar.jobs.core.PulsarJob
import ai.platon.pulsar.persist.graph.GraphGroupKey
import ai.platon.pulsar.persist.graph.GraphGroupKey.*
import ai.platon.pulsar.persist.io.WebGraphWritable
import kotlin.system.exitProcess

class Out2InGraphUpdateJob: WebGraphUpdateJob() {

    public override fun initJob() {
        // Partition by {url}, sort by {url,score} and group by {url}.
        // This ensures that the inlinks are sorted by score when they enter the reducer.
        currentJob.partitionerClass = UrlOnlyPartitioner::class.java
        currentJob.setSortComparatorClass(GraphKeyComparator::class.java)
        currentJob.setGroupingComparatorClass(UrlOnlyComparator::class.java)

        // currentJob.setCombinerClass(OutGraphUpdateCombiner.class);
        // currentJob.setCombinerKeyGroupingComparatorClass(UrlOnlyComparator.class);
        val fields = getFields(currentJob)
        PulsarJob.initMapper(currentJob, fields, GraphGroupKey::class.java, WebGraphWritable::class.java,
                Out2InUpdateMapper::class.java, getBatchIdFilter(options.batchId))
        PulsarJob.initReducer(currentJob, Out2InUpdateReducer::class.java)
    }
}

fun main(args: Array<String>) {
    JobEnv.initialize()
    val res = AppContextAwareJob.run(Out2InGraphUpdateJob(), args)
    exitProcess(res)
}
