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

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Creates and caches [Protocol] plugins. Protocol plugins should define
 * the attribute "protocolName" with the name of the protocol that they
 * implement. Configuration object is used for caching. Cache key is constructed
 * from appending protocol name (eg. http) to constant
 */
@Component
class ProtocolFactory(private val immutableConfig: ImmutableConfig) : AutoCloseable {
    private val log = LoggerFactory.getLogger(ProtocolFactory::class.java)

    private val protocols: MutableMap<String, Protocol> = ConcurrentHashMap()
    private val closed = AtomicBoolean()

    init {
        ResourceLoader.readAllLines("protocol-plugins.txt")
                .asSequence()
                .map { it.trim() }
                .filterNot { it.startsWith("#") }
                .map { it.split("\\s+".toRegex()) }
                .filter { it.size >= 2 }
                .map { it[0] to getInstance(it) }
                .filter { it.second != null }
                .associate { it.first to it.second!! }
                .onEach { it.value.conf = immutableConfig }
                .toMap(protocols)
        protocols.keys.joinToString(", ", "Supported protocols: ", "")
                .also { log.info(it) }
    }

    /**
     * TODO: configurable, using major protocol/sub protocol is a good idea
     * Using major protocol/sub protocol is a good idea, for example:
     * selenium:http://www.baidu.com/
     * jdbc:h2:tcp://localhost/~/test
     */
    fun getProtocol(page: WebPage): Protocol {
        var mode = page.fetchMode
        if (mode == FetchMode.UNKNOWN) {
            mode = FetchMode.BROWSER
        }
        return when (mode.also { page.fetchMode = it }) {
            FetchMode.BROWSER -> getProtocol("browser:" + page.url)
            else -> getProtocol(page.url)
        }?:throw ProtocolNotFound(page.url)
    }

    /**
     * Returns the appropriate [Protocol] implementation for a url.
     *
     * @param url The url
     * @return The appropriate [Protocol] implementation for a given
     * [URL].
     */
    fun getProtocol(url: String): Protocol? {
        val protocolName = StringUtils.substringBefore(url, ":")
        // sub protocol can be supported by main:sub://example.com later
        return protocols[protocolName]
    }

    fun getProtocol(mode: FetchMode): Protocol? {
        return getProtocol(mode.name.lowercase(Locale.getDefault()) + "://")
    }

    private fun getInstance(config: List<String>): Protocol? {
        try {
            // config[0] is the protocol name, config[1] is the class name, and the rest are properties
            val className = config[1]
            return Class.forName(className).constructors.first().newInstance() as Protocol
        } catch (e: ClassNotFoundException) {
            log.error(e.stringify())
        } catch (e: InstantiationException) {
            log.error(e.stringify())
        } catch (e: IllegalAccessException) {
            log.error(e.stringify())
        }
        return null
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            protocols.values.forEach { protocol: Protocol ->
                try {
                    protocol.close()
                } catch (e: Throwable) {
                    log.error(e.toString())
                }
            }
            protocols.clear()
        }
    }
}
