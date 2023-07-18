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

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebDBException
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.query.Result
import org.slf4j.LoggerFactory
import java.util.function.Predicate

class DbIterator(
    val result: Result<String, GWebPage>,
    val conf: ImmutableConfig
) : Iterator<WebPage> {
    private val log = LoggerFactory.getLogger(WebDb::class.java)

    private var nextPage: WebPage? = null
    var filter: Predicate<WebPage>? = null

    init {
        try {
            moveToNext()
        } catch (e: Exception) {
            log.error("Failed to create read iterator", e)
        }
    }

    override fun hasNext(): Boolean {
        return nextPage != null
    }

    @Throws(WebDBException::class)
    override fun next(): WebPage {
        try {
            moveToNext()
            if (!hasNext()) {
                result.close()
            }
        } catch (e: Exception) {
            log.error("Failed to move to the next record", e)
        }

        return nextPage ?: WebPage.NIL
    }

    @Throws(WebDBException::class)
    private fun moveToNext() {
        try {
            moveToNext0()
        } catch (e: Exception) {
            val message = "Data storage failure | [moveToNext]"
            throw WebDBException(message, e)
        }
    }

    @Throws(Exception::class)
    private fun moveToNext0() {
        nextPage = null
        while (nextPage == null && result.next()) {
            val page = WebPage.box(result.key, result.get(), true, conf.toVolatileConfig())
            val f = filter
            if (f == null || f.test(page)) {
                nextPage = page
            }
        }
    }
}
