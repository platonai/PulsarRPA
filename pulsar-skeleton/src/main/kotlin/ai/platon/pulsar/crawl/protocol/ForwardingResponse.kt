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
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata

/**
 * Forward a response.
 */
open class ForwardingResponse(
        page: WebPage,
        status: ProtocolStatus,
        headers: MultiMetadata,
        content: ByteArray
) : Response(page, status, headers, content) {
    /**
     * The page should keep status unchanged
     */
    constructor(status: ProtocolStatus, page: WebPage) : this("", status, MultiMetadata(), page)

    /**
     * The page should keep status unchanged
     */
    constructor(retryScope: RetryScope?, page: WebPage) : this("", ProtocolStatus.retry(retryScope), MultiMetadata(), page)

    /**
     * The page should keep status unchanged
     */
    constructor(e: Throwable?, page: WebPage) : this("", ProtocolStatus.failed(e), MultiMetadata(), page)

    /**
     * The page should keep status unchanged
     */
    constructor(content: String, status: ProtocolStatus, headers: MultiMetadata, page: WebPage) 
            : this(page, status, headers, content.toByteArray())

    companion object {
        fun canceled(page: WebPage): ForwardingResponse = ForwardingResponse(ProtocolStatus.STATUS_CANCELED, page)
        fun retry(page: WebPage, retryScope: RetryScope?): ForwardingResponse = ForwardingResponse(retryScope, page)
        fun failed(page: WebPage, e: Throwable?): ForwardingResponse = ForwardingResponse(e, page)
    }
}
