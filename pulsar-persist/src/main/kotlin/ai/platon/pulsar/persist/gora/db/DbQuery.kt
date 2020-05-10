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
package ai.platon.pulsar.persist.gora.db

import ai.platon.pulsar.common.config.AppConstants
import org.apache.avro.util.Utf8
import java.util.*

class DbQuery {
    private var crawlId: String? = null
    private var batchId = Utf8(AppConstants.ALL_BATCHES)
    var startUrl: String? = null
    var endUrl: String? = null
    var urlFilter = "+."
    var start = 0L
    var limit = 100L
    var fields: HashSet<String> = HashSet()

    private constructor() {}
    constructor(startUrl: String) {
        this.startUrl = startUrl
        endUrl = null
    }

    constructor(startUrl: String, endUrl: String?) {
        this.startUrl = startUrl
        this.endUrl = endUrl
    }

    constructor(crawlId: String, batchId: String, startUrl: String? = null, endUrl: String? = null) {
        this.crawlId = crawlId
        this.batchId = Utf8(batchId)
        this.startUrl = startUrl
        this.endUrl = endUrl
    }

    fun getCrawlId(): String {
        return if (crawlId == null) "" else crawlId!!
    }

    fun setCrawlId(crawlId: String) {
        this.crawlId = crawlId
    }

    fun getBatchId(): CharSequence {
        return batchId
    }

    fun setBatchId(batchId: CharSequence) {
        this.batchId = Utf8(batchId.toString())
    }
}
