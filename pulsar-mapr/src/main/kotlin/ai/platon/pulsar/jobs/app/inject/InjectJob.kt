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
package ai.platon.pulsar.jobs.app.inject

import ai.platon.pulsar.common.HdfsUtils
import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.InjectOptions
import ai.platon.pulsar.jobs.JobEnv
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.commons.io.FileUtils
import org.apache.gora.mapreduce.GoraOutputFormat
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Reducer
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class InjectJob : AppContextAwareJob() {

    @Throws(Exception::class)
    public override fun setup(params: Params) {
        val crawlId = params[PulsarParams.ARG_CRAWL_ID, jobConf[CapabilityTypes.STORAGE_CRAWL_ID]]
        var seeds = params[PulsarParams.ARG_SEEDS, ""]
        if (seeds.startsWith("@")) {
            seeds = String(Files.readAllBytes(Paths.get(seeds.substring(1))))
        }
        val configuredUrls = StringUtil.getUnslashedLines(seeds).filter { it.isNotBlank() && !it.startsWith("#") }
                .sorted().distinct().toList()
        // And also save it in HDFS as normal file
        val seedFile = File.createTempFile("seed", ".txt")
        FileUtils.writeLines(seedFile, configuredUrls)
        val seedPath = seedFile.absolutePath
        if (HdfsUtils.isDistributedFS(jobConf)) {
            LOG.info("Running under hadoop distributed file system, copy seed file onto HDFS")
            HdfsUtils.copyFromLocalFile(seedPath, jobConf)
        }
        jobConf[CapabilityTypes.STORAGE_CRAWL_ID] = crawlId
        jobConf[CapabilityTypes.INJECT_SEED_PATH] = seedPath
        LOG.info(Params.format(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId,
                "seedPath", seedPath
        ))
    }

    public override fun initJob() {
        val conf = getJobConf().unbox()
        val seedPath = conf[CapabilityTypes.INJECT_SEED_PATH]
        FileInputFormat.addInputPath(currentJob, Path(seedPath))
        currentJob.mapperClass = InjectMapper::class.java
        currentJob.mapOutputKeyClass = String::class.java
        currentJob.mapOutputValueClass = GWebPage::class.java
        currentJob.outputFormatClass = GoraOutputFormat::class.java
        currentJob.reducerClass = Reducer::class.java
        GoraOutputFormat.setOutput(currentJob, webDb.store, true)
        currentJob.numReduceTasks = 0
    }

    override fun run(args: Array<String>): Int {
        if (args.isEmpty()) {
            LOG.error("Seed urls are required")
            return -1
        }

        val opts = InjectOptions(args)
        opts.crawlId = jobConf[CapabilityTypes.STORAGE_CRAWL_ID, ""]
        opts.parseOrExit()

        run(opts.params)

        return 0
    }

    companion object {
        val LOG = LoggerFactory.getLogger(InjectJob::class.java)
    }
}

fun main(args: Array<String>) {
    JobEnv.initialize()
    val res = AppContextAwareJob.run(InjectJob(), args)
    exitProcess(res)
}
