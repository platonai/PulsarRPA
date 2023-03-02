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
import java.lang.ref.WeakReference
import java.util.*

/**
 * The page datum collected from a real page to update a WebPage.
 * */
class PageDatum(
    /**
     * The url is the permanent internal address, and it's also the storage key.
     * The url can differ from the original url passed by the user, because the original url might be normalized,
     * and the url also can differ from the final location of the page, because the page can be redirected in the browser.
     */
    val url: String,
    /**
     * Returns the document location as a string.
     *
     * [location] is the last working address, retrieved by javascript,
     * it might redirect from the original url, or it might have additional query parameters.
     * [location] can differ from [url].
     *
     * In javascript, the documentURI property can be used on any document types. The document.URL
     * property can only be used on HTML documents.
     *
     * @see <a href='https://www.w3schools.com/jsref/prop_document_documenturi.asp'>
     *     HTML DOM Document documentURI</a>
     * */
    var location: String = url,
    /**
     * The protocol status without translation
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
     * Protocol-specific headers.
     */
    val headers: MultiMetadata = MultiMetadata(),
    /**
     * Other protocol-specific data.
     */
    val metadata: MultiMetadata = MultiMetadata(),
) {
    /**
     * The page category, it can be specified by the user or detected automatically
     * */
    var pageCategory: OpenPageCategory? = null
    /**
     * The proxy entry used to fetch the page
     * */
    var proxyEntry: ProxyEntry? = null
    /**
     * The browser type used to fetch the page
     * */
    var lastBrowser: BrowserType? = null
    /**
     * The html content integrity
     * */
    var htmlIntegrity: HtmlIntegrity? = null
    /**
     * Track the DOM states at different time points in a real browser, which are calculated by javascript.
     * */
    var activeDOMStatTrace: ActiveDOMStatTrace? = null
    /**
     * The page URLs in a real browser calculated by javascript.
     * */
    var activeDOMUrls: ActiveDOMUrls? = null
    /**
     * The length of the original page content in bytes, the content has no inserted pulsar metadata.
     */
    var originalContentLength: Int = -1
    /**
     * The length of the final page content in bytes, the content might has inserted pulsar metadata.
     */
    val contentLength get() = (content?.size ?: 0).toLong()

    var page = WeakReference<WebPage>(null)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return other is PageDatum
                && url == other.url
                && location == other.location
                && contentType == other.contentType
                && Arrays.equals(content, other.content)
    }

    override fun hashCode() = url.hashCode()

    override fun toString() = url
}
