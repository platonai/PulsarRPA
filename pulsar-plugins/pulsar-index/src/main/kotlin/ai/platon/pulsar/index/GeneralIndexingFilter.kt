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
package ai.platon.pulsar.index

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.index.IndexingException
import ai.platon.pulsar.crawl.index.IndexingFilter
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GFieldGroup
import org.apache.commons.lang3.StringUtils
import java.util.function.Consumer

/**
 * Adds basic searchable fields to a document.
 */
class GeneralIndexingFilter(
    override var conf: ImmutableConfig,
) : IndexingFilter {

    /**
     */
    override fun setup(conf: ImmutableConfig) {
        this.conf = conf
        maxContentLength = conf.getInt("index.max.content.length", 10 * 10000)
        IndexingFilter.LOG.info(params.formatAsLine())
    }

    override fun getParams(): Params {
        return Params.of(
            "className", this.javaClass.simpleName,
            "maxContentLength", maxContentLength
        )
    }

    /**
     * @param doc  The [IndexDocument] object
     * @param url  URL to be filtered for anchor text
     * @param page [WebPage] object relative to the URL
     * @return filtered IndexDocument
     */
    @Throws(IndexingException::class)
    override fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        doc.addIfAbsent("id", doc.key)
        doc.addIfAbsent("url", url)
        doc.addIfAbsent("seed_url", StringUtils.substringBefore(page.args, " "))
        addDocFields(doc, url, page)
        return doc
    }

    private fun addDocFields(doc: IndexDocument, url: String, page: WebPage) {
        page.pageModel?.fieldGroups?.forEach { addDocFields(doc, it.fields) }
    }

    private fun addDocFields(doc: IndexDocument, fields: Map<CharSequence, CharSequence?>) {
        fields.filter { it.value != null }.map { it.key.toString() to it.value.toString() }
            .filter { it.second.length < maxContentLength }
            .forEach { doc.addIfAbsent(it.first, it.second) }
    }

    companion object {
        private var maxContentLength = 20_000
    }
}
