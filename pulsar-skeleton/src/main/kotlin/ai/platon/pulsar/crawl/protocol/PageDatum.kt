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

import ai.platon.pulsar.common.MimeUtil
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.metadata.MultiMetadata
import java.util.*

/**
 * The page datum to update a WebPage
 * */
class PageDatum {
    /**
     * The permanent internal address, the storage key, and is the same as the page's url if not redirected
     */
    var url: String? = null
        private set
    /**
     * The base url for relative links contained in the content. Maybe be
     * different from url if the request redirected
     */
    var location: String? = null
        private set
    /**
     * The binary content retrieved.
     */
    var content: ByteArray? = null
    /**
     * The media type of the retrieved content.
     */
    var contentType: String? = null
    /**
     * Other protocol-specific data.
     */
    var metadata: MultiMetadata

    val length get() = (content?.size?:0).toLong()

    private var mimeTypes: MimeUtil? = null

    constructor() {
        metadata = MultiMetadata()
    }

    /**
     * The url is the permanent internal address, it might not still available to access the target.
     *
     * location is the last working address, it might redirect to url, or it might have additional random parameters.
     *
     * location may be different from url, it's generally normalized.
     */
    constructor(url: String, location: String, content: ByteArray, contentType: String?, metadata: MultiMetadata,
                conf: ImmutableConfig) {
        this.url = url
        this.location = location
        this.content = content
        this.metadata = metadata
        mimeTypes = MimeUtil(conf)
        this.contentType = getContentType(contentType, url, content)
    }

    // The location where the HTML was retrieved from, to resolve relative links against.
    constructor(url: String, location: String, content: ByteArray?, contentType: String?, metadata: MultiMetadata,
                mimeTypes: MimeUtil?) {
        this.url = url
        this.location = location
        this.content = content
        this.metadata = metadata
        this.mimeTypes = mimeTypes
        this.contentType = getContentType(contentType, url, content)
    }

    override fun equals(other: Any?): Boolean {
        return other is PageDatum
                && url == other.url
                && location == other.location
                && Arrays.equals(content, other.content)
                && contentType == other.contentType
                && metadata == other.metadata
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    private fun getContentType(typeName: String?, url: String, data: ByteArray?): String? {
        return mimeTypes?.autoResolveContentType(typeName, url, data)
    }

    override fun toString(): String {
        return Params.of(
                "version", serializeId,
                "url", url,
                "location", location,
                "metadata", metadata,
                "contentType", contentType,
                "content", String(content!!)
        ).formatAsLine()
    }

    companion object {
        val EMPTY_CONTENT = ByteArray(0)
        val serializeId = 0L
    }
}
