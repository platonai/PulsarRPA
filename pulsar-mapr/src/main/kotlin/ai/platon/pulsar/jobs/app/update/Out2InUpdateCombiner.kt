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

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.graph.GraphGroupKey
import ai.platon.pulsar.persist.graph.WebGraph
import ai.platon.pulsar.persist.io.WebGraphWritable
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.Reducer
import org.slf4j.LoggerFactory

internal class Out2InUpdateCombiner : Reducer<GraphGroupKey, WebGraphWritable, GraphGroupKey, WebGraphWritable>() {
    private var edgeCountBeforeCombine = 0
    private var edgeCountAfterCombine = 0

    private lateinit var conf: Configuration
    private lateinit var webGraphWritable: WebGraphWritable

    override fun setup(context: Context) {
        conf = context.configuration
        webGraphWritable = WebGraphWritable(WebGraph.EMPTY, WebGraphWritable.OptimizeMode.IGNORE_TARGET, conf)
        Params.of(
                "className", this.javaClass.simpleName
        ).withLogger(LOG).info()
    }

    override fun reduce(key: GraphGroupKey, subGraphs: Iterable<WebGraphWritable>, context: Context) {
        val graph = WebGraph()
        try {
            for (graphWritable in subGraphs) {
                edgeCountBeforeCombine += graphWritable.graph.edgeSet().size
                graph.combine(graphWritable.graph)
            }
            edgeCountAfterCombine += graph.edgeSet().size
            context.write(key, webGraphWritable.reset(graph))
        } catch (e: Throwable) {
            LOG.error(StringUtil.stringifyException(e))
        }
    }

    override fun cleanup(context: Context) {
        LOG.info("Edge count in combiner : $edgeCountBeforeCombine -> $edgeCountAfterCombine")
    }

    companion object {
        val LOG = LoggerFactory.getLogger(Out2InUpdateCombiner::class.java)
    }
}
