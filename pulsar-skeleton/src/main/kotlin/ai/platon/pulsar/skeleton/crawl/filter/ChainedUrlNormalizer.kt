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
package ai.platon.pulsar.skeleton.crawl.filter

import org.apache.commons.collections4.MultiValuedMap
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap

class ChainedUrlNormalizer : AbstractScopedUrlNormalizer() {

    var maxLoops = 1

    val urlNormalizers: MultiValuedMap<String, ScopedUrlNormalizer> = ArrayListValuedHashMap()

    fun add(normalizer: ScopedUrlNormalizer, scope: String = SCOPE_DEFAULT) {
        urlNormalizers.put(scope, normalizer)
    }

    fun get(scope: String = SCOPE_DEFAULT): Collection<ScopedUrlNormalizer>? = urlNormalizers[scope]

    override fun isRelevant(url: String, scope: String) = true

    /**
     * Normalize
     *
     * @param url The URL string to normalize.
     * @return A normalized String, using the given `scope`
     */
    override fun normalize(url: String, scope: String): String? {
        // optionally loop several times, and break if no further changes
        val relevantNormalizers = urlNormalizers[scope]?.filter { it.isRelevant(url, scope) } ?: return url

        var target: String? = url
        var tmp = target
        for (k in 0 until maxLoops) {
            for (normalizer in relevantNormalizers) {
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
        return urlNormalizers.toString()
    }
}
