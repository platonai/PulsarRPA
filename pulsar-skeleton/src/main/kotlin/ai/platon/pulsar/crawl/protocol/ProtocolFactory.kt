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
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

/**
 * Creates and caches [Protocol] plugins. Protocol plugins should define
 * the attribute "protocolName" with the name of the protocol that they
 * implement. Configuration object is used for caching. Cache key is constructed
 * from appending protocol name (eg. http) to constant
 */
@Component
class ProtocolFactory(private val immutableConfig: ImmutableConfig) : AutoCloseable {
    private val protocols: MutableMap<String, Protocol> = ConcurrentHashMap()
    private val closed = AtomicBoolean()
    /**
     * TODO: configurable, using major protocol/sub protocol is a good idea
     * Using major protocol/sub protocol is a good idea, for example:
     * selenium:http://www.baidu.com/
     * jdbc:h2:tcp://localhost/~/test
     */
    fun getProtocol(page: WebPage): Protocol {
        var mode = page.fetchMode
        if (mode == null || mode == FetchMode.UNKNOWN) {
            mode = FetchMode.BROWSER
        }
        return when (mode.also { page.fetchMode = it }) {
            FetchMode.BROWSER -> getProtocol("selenium:" + page.url)
            FetchMode.CROWD_SOURCING -> getProtocol("crowd:" + page.url)
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
        return getProtocol(mode.name.toLowerCase() + "://")
    }

    private fun getInstance(config: Array<String>): Protocol? {
        try {
            // config[0] is the protocol name, config[1] is the class name, and the rest are properties
            val className = config[1]
            return Class.forName(className).newInstance() as Protocol
        } catch (e: ClassNotFoundException) {
            LOG.error(Strings.stringifyException(e))
        } catch (e: InstantiationException) {
            LOG.error(Strings.stringifyException(e))
        } catch (e: IllegalAccessException) {
            LOG.error(Strings.stringifyException(e))
        }
        return null
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            protocols.values.forEach { protocol: Protocol ->
                try {
                    protocol.close()
                } catch (e: Throwable) {
                    LOG.error(e.toString())
                }
            }
            protocols.clear()
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(ProtocolFactory::class.java)
    }

    init {
        val results = ResourceLoader.readAllLines("protocol-plugins.txt")
                .map { it.trim { it <= ' ' } }
                .filter { !it.startsWith("#") }
                .map { it.split("\\s+".toRegex()).toTypedArray() }
                .filter { it.size >= 2 }
                .map { it[0] to getInstance(it) }
                .filter { it.second != null }
                .onEach { it.second!!.conf = immutableConfig }
                .associate { it.first to it.second!! }
        protocols.putAll(results)
        LOG.info(protocols.keys.stream()
                .collect(Collectors.joining(", ", "Supported protocols: ", "")))
    }
}
