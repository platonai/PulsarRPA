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
package ai.platon.pulsar.jobs.app.parse

import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.jobs.common.IdentityPageReducer
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import ai.platon.pulsar.jobs.core.PulsarJob
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.slf4j.LoggerFactory
import java.util.*

class ParserJob : AppContextAwareJob() {
    private var batchId: String? = null

    companion object {
        @JvmField
        val LOG = LoggerFactory.getLogger(ParserJob::class.java)
        private val FIELDS: HashSet<GWebPage.Field> = HashSet()
        fun getFields(conf: ImmutableConfig): Collection<GWebPage.Field> {
            return FIELDS
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val configLocation = System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, AppConstants.JOB_CONTEXT_CONFIG_LOCATION)
            val res = PulsarJob.run(ImmutableConfig(), ParserJob(), args)
            System.exit(res)
        }

        init {
            Collections.addAll(FIELDS, *GWebPage.Field.values())
        }
    }

    @Throws(Exception::class)
    public override fun setup(params: Params) {
        val crawlId = params[PulsarParams.ARG_CRAWL_ID, jobConf[CapabilityTypes.STORAGE_CRAWL_ID]]
        val fetchMode = jobConf[CapabilityTypes.FETCH_MODE]
        batchId = params[PulsarParams.ARG_BATCH_ID, AppConstants.ALL_BATCHES]
        val reparse = batchId.equals("-reparse", ignoreCase = true)
        batchId = if (reparse) AppConstants.ALL_BATCHES else batchId
        val limit = params.getInt(PulsarParams.ARG_LIMIT, -1)
        val resume = params.getBoolean(PulsarParams.ARG_RESUME, false)
        val force = params.getBoolean(PulsarParams.ARG_FORCE, false)

        jobConf[CapabilityTypes.STORAGE_CRAWL_ID] = crawlId
        jobConf[CapabilityTypes.BATCH_ID] = batchId
        jobConf.setInt(CapabilityTypes.LIMIT, limit)
        jobConf.setBoolean(CapabilityTypes.RESUME, resume)
        jobConf.setBoolean(CapabilityTypes.FORCE, force)
        jobConf.setBoolean(CapabilityTypes.PARSE_REPARSE, reparse)

        LOG.info(Params.format(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId,
                "batchId", batchId,
                "fetchMode", fetchMode,
                "resume", resume,
                "force", force,
                "reparse", reparse,
                "limit", limit
        ))
    }

    @Throws(Exception::class)
    public override fun initJob() {
        val fields = getFields(getJobConf())
        val batchIdFilter = getBatchIdFilter(batchId)
        PulsarJob.initMapper(currentJob, fields, String::class.java, GWebPage::class.java, ParserMapper::class.java, batchIdFilter)
        PulsarJob.initReducer(currentJob, IdentityPageReducer::class.java)
        // there is no reduce phase, so set reduce tasks to be 0
        currentJob.numReduceTasks = 0
    }

    private fun printUsage() {
        System.err.println("Usage: ParserJob (<batchId> | -reparse) [-crawlId <id>] [-resume] [-force]")
        System.err.println("    <batchId>     - symbolic batch ID created by Generator")
        System.err.println("    -reparse      - reparse pages from all crawl jobs")
        System.err.println("    -crawlId <id> - the id to prefix the schemas to operate on, \n \t \t    (default: storage.crawl.id)")
        System.err.println("    -limit        - limit")
        System.err.println("    -resume       - resume a previous incomplete job")
        System.err.println("    -force        - force re-parsing even if a page is already parsed")
    }

    @Throws(Exception::class)
    override fun run(args: Array<String>): Int {
        if (args.size < 1) {
            printUsage()
            return -1
        }
        val conf: ImmutableConfig = getJobConf()
        val batchId = args[0]
        if (batchId.startsWith("-")) {
            printUsage()
            return -1
        }
        var crawlId = conf[CapabilityTypes.STORAGE_CRAWL_ID, ""]
        var limit = -1
        var resume = false
        var force = false
        var i = 0
        while (i < args.size) {
            if ("-crawlId" == args[i]) {
                crawlId = args[++i]
                // getConf().set(CRAWL_ID, args[++i]);
            } else if ("-limit" == args[i]) {
                limit = args[++i].toInt()
            } else if ("-resume" == args[i]) {
                resume = true
            } else if ("-force" == args[i]) {
                force = true
            }
            i++
        }
        parse(crawlId, batchId, limit, resume, force)
        return 0
    }

    fun parse(crawlId: String?, batchId: String?, limit: Int, resume: Boolean, force: Boolean) {
        run(Params.of(
                PulsarParams.ARG_CRAWL_ID, crawlId,
                PulsarParams.ARG_BATCH_ID, batchId,
                PulsarParams.ARG_RESUME, resume,
                PulsarParams.ARG_FORCE, force,
                PulsarParams.ARG_LIMIT, if (limit > 0) limit else null
        ))
    }
}