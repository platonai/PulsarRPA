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
package ai.platon.pulsar.crawl.filter

import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.*

/**
 * This class uses a "chained filter" pattern to run defined normalizers.
 * Different lists of normalizers may be defined for different "scopes", or
 * contexts where they are used (note however that they need to be activated
 * first through <tt>plugin.include</tt> property).
 *
 *
 *
 *
 * There is one global scope defined by default, which consists of all active
 * normalizers. The order in which these normalizers are executed may be defined
 * in "urlnormalizer.order" property, which lists space-separated implementation
 * classes (if this property is missing normalizers will be run in random
 * order). If there are more normalizers activated than explicitly named on this
 * list, the remaining ones will be run in random order after the ones specified
 * on the list are executed.
 *
 *
 *
 * You can define a set of contexts (or scopes) in which normalizers may be
 * called. Each scope can have its own list of normalizers (defined in
 * "urlnormalizer.scope.<scope_name>" property) and its own order (defined in
 * "urlnormalizer.order.<scope_name>" property). If any of these properties are
 * missing, default settings are used for the global scope.
</scope_name></scope_name> *
 *
 *
 * In case no normalizers are required for any given scope, a
 * `ai.platon.pulsar.crawl.net.urlnormalizer.pass.PassURLNormalizer` should
 * be used.
 *
 *
 *
 * Each normalizer may further select among many configurations, depending on
 * the scope in which it is called, because the scope name is passed as a
 * parameter to each normalizer. You can also use the same normalizer for many
 * scopes.
 *
 *
 *
 * Several scopes have been defined, and various PulsarConstants cli will attempt using
 * scope-specific normalizers first (and fall back to default config if
 * scope-specific configuration is missing).
 *
 *
 *
 * Normalizers may be run several times, to ensure that modifications introduced
 * by normalizers at the end of the list can be further reduced by normalizers
 * executed at the beginning. By default this loop is executed just once - if
 * you want to ensure that all possible combinations have been applied you may
 * want to run this loop up to the number of activated normalizers. This loop
 * count can be configured through <tt>urlnormalizer.loop.count</tt> property.
 * As soon as the url is unchanged the loop will stop and return the result.
 *
 *
 * @author Andrzej Bialecki
 */
class UrlNormalizers(
        val urlNormalizers: List<UrlNormalizer>,
        val scope: String = SCOPE_DEFAULT,
        val conf: ImmutableConfig
) {
    private val LOG = LoggerFactory.getLogger(UrlNormalizers::class.java)

    // Reserved
    private val scopedUrlNormalizers: Map<String, UrlNormalizer> = HashMap()
    private val maxLoops = conf.getInt("urlnormalizer.loop.count", 1)

    constructor(conf: ImmutableConfig): this(listOf(), SCOPE_DEFAULT, conf)

    /**
     * TODO : not implemented
     */
    fun getURLNormalizers(scope: String): List<UrlNormalizer> {
        return urlNormalizers
    }

    fun findByClassName(name: String): UrlNormalizer? {
        return urlNormalizers.firstOrNull { it.javaClass.simpleName == name || it.javaClass.name == name }
    }

    /**
     * Normalize
     *
     * @param urlString The URL string to normalize.
     * @param scope     The given scope.
     * @return A normalized String, using the given `scope`
     */
    /**
     * Normalize
     *
     * @param urlString The URL string to normalize.
     * @return A normalized String, using the given `scope`
     */
    @JvmOverloads
    fun normalize(url: String, scope: String = SCOPE_DEFAULT): String? {
        // optionally loop several times, and break if no further changes
        var target: String? = url
        var tmp = target
        for (k in 0 until maxLoops) {
            for (normalizer in urlNormalizers) {
                if (target == null) {
                    return null
                }
                target = normalizer.normalize(target, scope)
            }
            if (tmp == target) {
                break
            }
            tmp = target
        }
        return target
    }

    override fun toString(): String {
        return urlNormalizers.joinToString { it.javaClass.simpleName }
    }

    companion object {
        /**
         * Default scope. If no scope properties are defined then the configuration
         * for this scope will be used.
         */
        const val SCOPE_DEFAULT = "default"
        const val SCOPE_PARTITION = "partition"
        const val SCOPE_GENERATE_HOST_COUNT = "generate_host_count"
        const val SCOPE_INJECT = "inject"
        const val SCOPE_FETCHER = "fetcher"
        const val SCOPE_CRAWLDB = "crawldb"
        const val SCOPE_LINKDB = "linkdb"
        const val SCOPE_INDEXER = "index"
        const val SCOPE_OUTLINK = "outlink"
    }
}