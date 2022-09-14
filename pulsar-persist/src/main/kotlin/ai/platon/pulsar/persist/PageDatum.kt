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
package ai.platon.pulsar.persist

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.OpenPageCategory
import ai.platon.pulsar.persist.model.ActiveDOMStatTrace
import ai.platon.pulsar.persist.model.ActiveDOMUrls
import java.util.*

/**
 * The page datum to update a WebPage
 * */
class PageDatum(
    /**
     * The permanent internal address, the storage key, and is the same as the page's url if not redirected
     */
    val url: String,
    /**
     * The last working address, it might redirect to url, or it might have additional random parameters.
     * location may be different from url, it's generally normalized.
     */
    var location: String = url,
    /**
     * The protocol status
     * */
    var protocolStatus: ProtocolStatus = ProtocolStatus.STATUS_CANCELED,
    /**
     * The binary content retrieved.
     */
    var content: ByteArray? = null,
    /**
     * The media type of the retrieved content.
     */
    var contentType: String? = null,
    /**
     * Other protocol-specific data.
     */
    val headers: MultiMetadata = MultiMetadata(),
    /**
     * Other protocol-specific data.
     */
    val metadata: MultiMetadata = MultiMetadata(),
) {
    var pageCategory: OpenPageCategory? = null
    var proxyEntry: ProxyEntry? = null
    var lastBrowser: BrowserType? = null
    var htmlIntegrity: HtmlIntegrity? = null
    var activeDOMStatTrace: ActiveDOMStatTrace? = null
    var activeDOMUrls: ActiveDOMUrls? = null

    val contentLength get() = (content?.size ?: 0).toLong()

    override fun equals(other: Any?): Boolean {
        return other is PageDatum
                && url == other.url
                && location == other.location
                && contentType == other.contentType
                && Arrays.equals(content, other.content)
    }

    override fun hashCode() = url.hashCode()

    override fun toString() = url
}
