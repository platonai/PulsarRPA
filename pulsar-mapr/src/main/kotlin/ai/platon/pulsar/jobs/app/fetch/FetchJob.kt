/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.jobs.app.fetch

import ai.platon.pulsar.common.config.AppConstants.CRAWL_DEPTH_FIRST
import ai.platon.pulsar.common.config.AppConstants.CRAWL_STRICT_DEPTH_FIRST
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.FetchOptions
import ai.platon.pulsar.jobs.JobEnv
import ai.platon.pulsar.jobs.common.FetchEntryWritable
import ai.platon.pulsar.jobs.common.URLPartitioner.FetchEntryPartitioner
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import ai.platon.pulsar.jobs.core.PulsarJob
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.hadoop.io.IntWritable
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/**
 * Fetch job
 */
class FetchJob : AppContextAwareJob() {
    companion object {
        val LOG = LoggerFactory.getLogger(FetchJob::class.java)
        private val unrelatedFields = arrayOf(
                GWebPage.Field.CONTENT,
                GWebPage.Field.PAGE_TEXT,
                GWebPage.Field.CONTENT_TEXT
        )
        private val FIELDS = GWebPage.Field.values().filterNot { it in unrelatedFields }
    }

    private lateinit var options: FetchOptions

    public override fun setup(params: Params) {
        // Recomputed config variables, this config will be passed to Mapper and Reducer
        jobConf[STORAGE_CRAWL_ID] = options.crawlId
        jobConf[BATCH_ID] = options.batchId
        jobConf.setEnum(FETCH_MODE, options.fetchMode)
        jobConf[FETCH_CRAWL_PATH_STRATEGY] = if (options.strictDf) CRAWL_STRICT_DEPTH_FIRST else CRAWL_DEPTH_FIRST
        jobConf.setInt(FETCH_THREADS_FETCH, options.numFetchThreads)
        jobConf.setBoolean(RESUME, options.resume)
        jobConf.setInt(MAPPER_LIMIT, options.limit)
        jobConf.setInt(MAPREDUCE_JOB_REDUCES, options.numReduceTasks)
        jobConf.setBoolean(INDEXER_JIT, options.index)
        if (options.index) {
            jobConf.set(INDEXER_ZK, options.zkHostString)
            jobConf.set(INDEXER_URL, options.indexerUrl)
            jobConf.set(INDEXER_COLLECTION, options.indexerCollection)
        }

        Params.of(
                "className", this.javaClass.simpleName
        ).merge(options.toParams()).withLogger(LOG).info()
    }

    public override fun initJob() {
        // For politeness, don't permit parallel execution of a single task
        currentJob.setReduceSpeculativeExecution(false)
        val batchId = options.batchId
        LOG.info("Applying batch id filter $batchId")
        val batchIdFilter = getBatchIdFilter(batchId)
        PulsarJob.initMapper(currentJob, FIELDS, IntWritable::class.java, FetchEntryWritable::class.java,
                FetchMapper::class.java, FetchEntryPartitioner::class.java, batchIdFilter, false)
        PulsarJob.initReducer(currentJob, FetchReducer::class.java)
        currentJob.numReduceTasks = options.numReduceTasks
        LOG.debug(FIELDS.joinToString(",", "Loaded Fields : ") { it.name })
    }

    override fun run(args: Array<String>): Int {
        options = FetchOptions(jobConf)
        options.parse(args)

        if (options.isHelp) {
            return 0
        }
        run(Params.EMPTY_PARAMS)
        return 0
    }
}

fun main(args: Array<String>) {
    JobEnv.initialize()
    val res = AppContextAwareJob.run(FetchJob(), args)
    exitProcess(res)
}
