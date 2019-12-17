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

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.UpdateOptions
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.hadoop.mapreduce.Job
import org.slf4j.LoggerFactory

abstract class WebGraphUpdateJob : AppContextAwareJob() {
    companion object {
        val log = LoggerFactory.getLogger(WebGraphUpdateJob::class.java)
        val unrelatedFields = arrayOf(GWebPage.Field.CONTENT,
                GWebPage.Field.PAGE_TEXT,
                GWebPage.Field.CONTENT_TEXT,
                GWebPage.Field.LINKS,
                GWebPage.Field.PAGE_MODEL)

        val FIELDS = GWebPage.Field.values().filterNot { it in unrelatedFields }
    }

    fun getFields(job: Job): Collection<GWebPage.Field> {
        return FIELDS
    }

    protected lateinit var options: UpdateOptions

    public override fun setup(params: Params) {
        jobConf[CapabilityTypes.STORAGE_CRAWL_ID] = options.crawlId
        jobConf[CapabilityTypes.BATCH_ID] = options.batchId
        jobConf.setInt(CapabilityTypes.LIMIT, options.limit)

        log.info(Params.format(
                "className", this.javaClass.simpleName,
                "round", options.round,
                "crawlId", options.crawlId,
                "batchId", options.batchId[0],
                "limit", options.limit
        ))
    }

    @Throws(Exception::class)
    override fun run(args: Array<String>): Int {
        options = UpdateOptions(args, jobConf)
        options.parseOrExit()
        run(options.params)
        return 0
    }
}
