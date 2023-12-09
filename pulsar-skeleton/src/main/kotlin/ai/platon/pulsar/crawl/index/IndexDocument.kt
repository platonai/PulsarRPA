/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.index

import ai.platon.pulsar.common.DateTimes.isoInstantFormat
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlUtils.unreverseUrl
import ai.platon.pulsar.crawl.scoring.ScoringFilters
import ai.platon.pulsar.persist.WebPage
import org.apache.commons.lang3.StringUtils
import java.time.Instant
import java.util.*

/**
 * A [IndexDocument] is the unit of indexing.
 */
class IndexDocument(
    var key: String = "",
    var url: String = "",
    val fields: MutableMap<String, IndexField> = mutableMapOf(),
    var weight: Float = 1.0f,
) {
    constructor(key: String) : this(key, unreverseUrl(key))

    fun addIfAbsent(name: String, value: Any) {
        fields.computeIfAbsent(name) { IndexField(value) }
    }

    fun addIfNotEmpty(name: String, value: String) {
        if (value.isEmpty()) {
            return
        }

        var field: IndexField? = fields.get(name)
        if (field == null) {
            field = IndexField(value)
            fields[name] = field
        } else {
            field.add(value)
        }
    }

    fun addIfNotNull(name: String, value: Any?) {
        if (value == null) {
            return
        }
        var field: IndexField? = fields[name]
        if (field == null) {
            field = IndexField(value)
            fields[name] = field
        } else {
            field.add(value)
        }
    }

    fun add(name: String, value: Any?) {
        var field: IndexField? = fields[name]
        if (field == null) {
            field = IndexField((value)!!)
            fields[name] = field
        } else {
            field.add((value)!!)
        }
    }

    fun getFieldValue(name: CharSequence): Any? {
        val field: IndexField? = fields[name]
        if (field == null) {
            return null
        }
        if (field.getValues().isEmpty()) {
            return null
        }
        return field.getValues()[0]
    }

    fun getField(name: CharSequence): IndexField? {
        return fields[name]
    }

    fun removeField(name: CharSequence): IndexField? {
        return fields.remove(name)
    }

    val fieldNames: Collection<CharSequence>
        get() = fields.keys

    fun getFieldValues(name: CharSequence): List<Any>? {
        val field: IndexField = fields[name] ?: return null
        return field.getValues()
    }

    fun getFieldValueAsString(name: CharSequence): String? {
        val field: IndexField? = fields[name]
        if (field == null || field.getValues().isEmpty()) {
            return null
        }
        return field.getValues().iterator().next().toString()
    }

    fun asMultimap(): Map<String, List<String>> {
        return fields.entries.associate { it.key to it.value.stringValues }
    }

    override fun toString(): String {
        val s: String = fields.entries.joinToString { "\t" + it.key + ":\t" + it.value }
        return "doc {\n$s\n}\n"
    }

    fun formatAsLine(): String {
        return fields.entries
            .map { "\t" + it.key + ":\t" + it.value }
            .joinToString { StringUtils.replaceChars(it, "[]", "") }
    }

    private fun format(obj: Any): String {
        if (obj is Date) {
            return isoInstantFormat(obj)
        } else if (obj is Instant) {
            return isoInstantFormat(obj)
        } else {
            return obj.toString()
        }
    }

    class Builder(private val conf: ImmutableConfig?) {
        private var indexingFilters: IndexingFilters
        private var scoringFilters: ScoringFilters
        fun with(indexingFilters: IndexingFilters): Builder {
            this.indexingFilters = indexingFilters
            return this
        }

        fun with(scoringFilters: ScoringFilters): Builder {
            this.scoringFilters = scoringFilters
            return this
        }

        /**
         * Index a [WebPage], here we add the following fields:
         *
         *  1. <tt>id</tt>: default uniqueKey for the [IndexDocument].
         *  1. <tt>digest</tt>: Digest is used to identify pages (like unique ID)
         * and is used to remove duplicates during the dedup procedure. It is
         * calculated
         *  1. <tt>batchId</tt>: The page belongs to a unique batchId, this is its
         * identifier.
         *  1. <tt>boost</tt>: Boost is used to calculate document (field) score
         * which can be used within queries submitted to the underlying indexing
         * library to find the best results. It's part of the scoring algorithms.
         * See scoring.link, scoring.opic, scoring.tld, etc.
         *
         *
         * @param key  The key of the page (reversed url).
         * @param page The [WebPage].
         * @return The indexed document, or null if skipped by index indexingFilters.
         */
        fun build(key: String?, page: WebPage?): IndexDocument? {
            if (key == null || page == null) {
                return null
            }
            var doc: IndexDocument? = IndexDocument(key)
            val url: String = doc!!.url
            doc = indexingFilters.filter((doc), url, page)
            // skip documents discarded by indexing indexingFilters
            if (doc == null) {
                return null
            }

            doc.addIfAbsent("id", key)
            doc.add("digest", page.signatureAsString)
            page.batchId?.let { doc.add("batchId", it) }
            var boost: Float = 1.0f
            // run scoring indexingFilters
            boost = scoringFilters.indexerScore(url, doc, page, boost)
            doc.weight = boost
            // store boost for use by explain and dedup
            doc.add("boost", boost.toString())
            return doc
        }

        companion object {
            fun newBuilder(conf: ImmutableConfig?): Builder {
                return Builder(conf)
            }
        }

        init {
            indexingFilters = IndexingFilters((conf)!!)
            scoringFilters = ScoringFilters((conf))
        }
    }
}
