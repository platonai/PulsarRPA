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
package ai.platon.pulsar.jobs.app.fetch

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.crawl.fetch.FetchMonitor
import ai.platon.pulsar.jobs.common.FetchEntryWritable
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer
import ai.platon.pulsar.jobs.core.HadoopReducerContext
import ai.platon.pulsar.jobs.fetch.service.FetchServer
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.hadoop.io.IntWritable

class FetchReducer : AppContextAwareGoraReducer<IntWritable, FetchEntryWritable, String, GWebPage>() {
    private lateinit var fetchMonitor: FetchMonitor
    private var fetchServer: FetchServer? = null

    override fun setup(context: Context) {
        super.setup(context)

        fetchMonitor = applicationContext.getBean(FetchMonitor::class.java)
        fetchMonitor.setup(jobConf)

        val crowdSourceModeIsEnabled = jobConf.getBoolean("fetch.crowd.source.mode.enabled", false)
        if (crowdSourceModeIsEnabled) {
            fetchServer = applicationContext.getBean("fetchServer", FetchServer::class.java)
            fetchServer?.setup(applicationContext)
        }
    }

    override fun doRun(context: Context) {
        fetchServer?.startAsDaemon()
        val reducerContext = HadoopReducerContext<IntWritable, FetchEntryWritable, String, GWebPage>(context)
        fetchMonitor.start(reducerContext)
    }

    override fun cleanupContext(context: Context) {
        try {
            fetchServer?.shutdownNow()
            fetchMonitor.close()

            super.cleanupContext(context)
        } catch (e: Throwable) {
            log.error(StringUtil.stringifyException(e))
        }
    }
}
