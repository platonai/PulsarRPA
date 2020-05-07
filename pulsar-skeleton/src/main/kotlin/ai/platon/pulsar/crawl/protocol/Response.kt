/**
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
package ai.platon.pulsar.crawl.protocol

import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import org.slf4j.LoggerFactory

/**
 * A response interface. Makes all protocols model HTTP
 * TODO: use PageDatum to hold data
 */
abstract class Response(
        val page: WebPage,
        /** The protocol's response status, with transform */
        val status: ProtocolStatus,
        /** All the headers */
        val headers: MultiMetadata,
        /** Returns the full content of the response */
        val content: ByteArray? = null,
        /** The page's last internet location */
        var location: String = page.url
) {
    /** The permanent internal address */
    val url get() = page.url
    /** The protocol's response code, it must be compatible with standard http response code */
    val httpCode get() = status.minorCode
    val length get() = content?.size?.toLong() ?: 0

    /** The value of a named header.  */
    fun getHeader(name: String): String? = headers[name]
}
