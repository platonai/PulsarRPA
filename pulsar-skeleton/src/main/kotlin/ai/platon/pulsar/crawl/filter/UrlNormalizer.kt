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

typealias NaiveUrlNormalizer = ai.platon.pulsar.common.urls.preprocess.UrlNormalizer

/**
 * Default scope. If no scope properties are defined then the configuration
 * for this scope will be used.
 */
const val SCOPE_DEFAULT: String = ""
const val SCOPE_PARTITION = "partition"
const val SCOPE_GENERATE_HOST_COUNT = "generate_host_count"
const val SCOPE_INJECT = "inject"
const val SCOPE_FETCHER = "fetcher"
const val SCOPE_CRAWLDB = "crawldb"
const val SCOPE_LINKDB = "linkdb"
const val SCOPE_INDEXER = "index"
const val SCOPE_OUTLINK = "outlink"

/**
 * Interface used to convert URLs to normal form and optionally perform
 * substitutions
 */
@Deprecated("Inappropriate name", ReplaceWith("ScopedUrlNormalizer"))
interface UrlNormalizer : NaiveUrlNormalizer {
    
    fun isRelevant(url: String, scope: String = SCOPE_DEFAULT): Boolean
    
    fun normalize(url: String, scope: String = SCOPE_DEFAULT): String?
    
    fun valid(urlString: String, scope: String): Boolean {
        return normalize(urlString, scope) != null
    }
}

interface ScopedUrlNormalizer : UrlNormalizer

abstract class AbstractScopedUrlNormalizer : ScopedUrlNormalizer {
    override fun isRelevant(url: String, scope: String): Boolean = false
    
    override fun invoke(url: String?) = url?.let { normalize(it) }
    
    abstract override fun normalize(url: String, scope: String): String?
}
