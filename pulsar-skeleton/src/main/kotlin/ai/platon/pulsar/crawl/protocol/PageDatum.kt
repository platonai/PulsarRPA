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

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.MimeTypeResolver
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.persist.model.ActiveDomMultiStatus
import ai.platon.pulsar.persist.model.ActiveDomUrls
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
        var status: ProtocolStatus = ProtocolStatus.STATUS_CANCELED,
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
        val metadata: MultiMetadata = MultiMetadata()
) {
    var pageCategory: PageCategory? = null
    var proxyEntry: ProxyEntry? = null
    var lastBrowser: BrowserType? = null
    var htmlIntegrity: HtmlIntegrity? = null
    var activeDomMultiStatus: ActiveDomMultiStatus? = null
    var activeDomUrls: ActiveDomUrls? = null

    val length get() = (content?.size?:0).toLong()

    constructor(url: String, location: String, content: ByteArray?, contentType: String?, metadata: MultiMetadata,
                mimeTypeResolver: MimeTypeResolver): this(url, location, ProtocolStatus.STATUS_CANCELED, content, contentType, metadata) {
        resolveMimeType(contentType, url, content, mimeTypeResolver)
    }

    fun resolveMimeType(contentType0: String?, url: String, data: ByteArray?, mimeTypeResolver: MimeTypeResolver) {
        this.contentType = mimeTypeResolver.autoResolveContentType(contentType0, url, data)
    }

    override fun equals(other: Any?): Boolean {
        return other is PageDatum
                && url == other.url
                && location == other.location
                && contentType == other.contentType
                && Arrays.equals(content, other.content)
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    override fun toString(): String {
        return Params.of(
                "serializeId", serializeId,
                "url", url,
                "location", location,
                "headers", headers,
                "metadata", metadata,
                "contentType", contentType
        ).formatAsLine()
    }

    companion object {
        val EMPTY_CONTENT = ByteArray(0)
        val serializeId = 0L
    }
}
