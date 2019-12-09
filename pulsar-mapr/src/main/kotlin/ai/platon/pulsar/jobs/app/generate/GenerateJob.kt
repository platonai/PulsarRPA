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
package ai.platon.pulsar.jobs.app.generate

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.AppFiles.readLastGeneratedRows
import ai.platon.pulsar.common.AppFiles.writeBatchId
import ai.platon.pulsar.common.AppFiles.writeLastGeneratedRows
import ai.platon.pulsar.common.URLUtil.GroupMode
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.common.options.GenerateOptions
import ai.platon.pulsar.jobs.JobEnv
import ai.platon.pulsar.jobs.app.fetch.FetchJob
import ai.platon.pulsar.jobs.common.JobUtils
import ai.platon.pulsar.jobs.common.SelectorEntry
import ai.platon.pulsar.jobs.common.URLPartitioner.SelectorEntryPartitioner
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import ai.platon.pulsar.jobs.core.PulsarJob
import ai.platon.pulsar.persist.gora.generated.GWebPage
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.time.Duration
import java.util.*
import kotlin.system.exitProcess

class GenerateJob : AppContextAwareJob {
    companion object {
        val LOG = LoggerFactory.getLogger(GenerateJob::class.java)
        private val unrelatedFields = arrayOf(
                GWebPage.Field.CONTENT,
                GWebPage.Field.PAGE_TEXT,
                GWebPage.Field.CONTENT_TEXT,
                GWebPage.Field.LINKS,
                GWebPage.Field.LIVE_LINKS,
                GWebPage.Field.INLINKS,
                GWebPage.Field.PAGE_MODEL
        )
        private val FIELDS = GWebPage.Field.values().filterNot { it in unrelatedFields }
    }

    private lateinit var options: GenerateOptions

    constructor() {}
    constructor(conf: ImmutableConfig) {
        setJobConf(conf)
    }

    /**
     * Transform arguments to configuration
     * */
    public override fun setup(params: Params) {
        // A pseudo current time can be passed to tell pulsar the pages should be updated
        val pseudoCurrTime = params.getLong(PulsarParams.ARG_CURTIME, startTime)
        val round = jobConf.getInt(CapabilityTypes.CRAWL_ROUND, 1)
        val maxDistance = jobConf.getUint(CapabilityTypes.CRAWL_MAX_DISTANCE, PulsarConstants.DISTANCE_INFINITE)
        val lastGeneratedRows = readLastGeneratedRows()
        var reGenerateSeeds = options.reGenerateSeeds
        if (!reGenerateSeeds) {
            reGenerateSeeds = RuntimeUtils.hasLocalFileCommand(PulsarConstants.CMD_FORCE_GENERATE_SEEDS)
        }
        val groupMode = jobConf.getEnum(CapabilityTypes.GENERATE_COUNT_MODE, GroupMode.BY_HOST)

        jobConf[CapabilityTypes.STORAGE_CRAWL_ID] = options.crawlId
        jobConf[CapabilityTypes.BATCH_ID] = options.batchId
        jobConf.setLong(CapabilityTypes.GENERATE_CUR_TIME, pseudoCurrTime)
        jobConf.setBoolean(CapabilityTypes.GENERATE_REGENERATE, options.reGenerate)
        jobConf.setBoolean(CapabilityTypes.GENERATE_REGENERATE_SEEDS, reGenerateSeeds)
        jobConf.setInt(CapabilityTypes.GENERATE_TOP_N, options.topN)
        jobConf.setInt(CapabilityTypes.GENERATE_LAST_GENERATED_ROWS, lastGeneratedRows)
        jobConf.setBoolean(CapabilityTypes.GENERATE_FILTER, !options.noFilter)
        jobConf.setBoolean(CapabilityTypes.GENERATE_NORMALISE, !options.noNormalizer)
        jobConf.setEnum(CapabilityTypes.PARTITION_MODE_KEY, groupMode)

        prepareSystemFiles(jobConf)

        LOG.info(Params.format(
                "className", this.javaClass.simpleName,
                "round", round,
                "crawlId", options.crawlId,
                "batchId", options.batchId,
                "filter", !options.noFilter,
                "normalize", !options.noNormalizer,
                "maxDistance", maxDistance,
                "topN", options.topN,
                "lastGeneratedRows", lastGeneratedRows,
                "reGenerate", options.reGenerate,
                "reGenerateSeeds", reGenerateSeeds,
                "groupMode", groupMode,
                "partitionMode", groupMode,
                "pseudoCurrTime", DateTimeUtil.format(pseudoCurrTime)
        ))
        printInfo()
    }

    private fun prepareSystemFiles(conf: ImmutableConfig) {
        if (HdfsUtils.isDistributedFS(conf)) {
            LOG.info("Running under hadoop distributed file system, copy files to HDFS")
            if (Files.exists(AppPaths.PATH_BANNED_URLS)) {
                HdfsUtils.copyFromLocalFile(AppPaths.PATH_BANNED_URLS.toString(), conf)
            }
        }
    }

    public override fun initJob() {
        PulsarJob.initMapper(currentJob, FIELDS, SelectorEntry::class.java,
                GWebPage::class.java, GenerateMapper::class.java, SelectorEntryPartitioner::class.java,
                inactiveFilter, false)
        PulsarJob.initReducer(currentJob, GenerateReducer::class.java)
    }

    override fun afterCleanup() {
        super.afterCleanup()
        try {
            val affectedRows = currentJob.counters.findCounter(CapabilityTypes.STAT_PULSAR_STATUS, "2'rPersist").value
            writeLastGeneratedRows(affectedRows)
        } catch (e: IOException) {
            LOG.error(e.toString())
        }
    }

    private fun printInfo() {
        val info = ("Supported File Commands : \n"
                + "1. force generate and re-fetch seeds next round : \n"
                + "echo " + PulsarConstants.CMD_FORCE_GENERATE_SEEDS + " > " + AppPaths.PATH_LOCAL_COMMAND + "\n"
                + "2. ban a url : \n"
                + "echo \"" + PulsarConstants.EXAMPLE_URL + "\" >> " + AppPaths.PATH_BANNED_URLS + "\n")
        LOG.info(info)
    }

    override fun run(args: Array<String>): Int {
        options = GenerateOptions(jobConf)
        options.parse(args)

        if (!options.isHelp) {
            run(options.toParams())
        }
        return 0
    }
}

fun main(args: Array<String>) {
    JobEnv.initialize()
    val res = AppContextAwareJob.run(GenerateJob(), args)
    exitProcess(res)
}
