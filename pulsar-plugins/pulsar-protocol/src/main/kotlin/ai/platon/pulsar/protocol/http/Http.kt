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
package ai.platon.pulsar.protocol.http

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.crawl.protocol.ProtocolException
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.crawl.protocol.http.AbstractNativeHttpProtocol
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.http.HttpResponse
import java.io.IOException
import java.net.URL

class Http: AbstractNativeHttpProtocol {

    constructor()

    constructor(conf: ImmutableConfig) {
        setConf(conf)
    }

    @Throws(ProtocolException::class, IOException::class, ProxyException::class)
    override fun getResponse(url: String, page: WebPage, redirect: Boolean): Response {
        return HttpResponse(this, URL(url), page)
    }
}
