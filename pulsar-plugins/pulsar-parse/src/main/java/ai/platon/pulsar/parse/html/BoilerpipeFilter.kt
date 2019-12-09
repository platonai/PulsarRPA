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
package ai.platon.pulsar.parse.html

import ai.platon.pulsar.boilerpipe.document.TextDocument
import ai.platon.pulsar.boilerpipe.extractors.ChineseNewsExtractor
import ai.platon.pulsar.boilerpipe.sax.SAXInput
import ai.platon.pulsar.boilerpipe.utils.ProcessingException
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.ParseFilter
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.crawl.parse.html.PrimerParser
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.PageCategory
import org.apache.commons.logging.LogFactory

/**
 * Parse html document into fields
 */
class BoilerpipeFilter(val conf: ImmutableConfig) : ParseFilter {
    private val primerParser = PrimerParser(conf)

    override fun filter(parseContext: ParseContext) {
        val page = parseContext.page
        val parseResult = parseContext.parseResult
        extract(page, page.getEncodingOrDefault("UTF-8"))
        parseResult.setSuccessOK()
    }

    /**
     * Extract the page into fields
     */
    fun extract(page: WebPage, encoding: String?): TextDocument? {
        val doc = extract(page) ?: return null
        page.contentTitle = doc.contentTitle
        page.contentText = doc.textContent
        page.pageCategory = PageCategory.valueOf(doc.pageCategoryAsString)
        page.updateContentPublishTime(doc.publishTime)
        page.updateContentModifiedTime(doc.modifiedTime)
        val id: Long = 1000
        page.pageModel.emplace(id, 0, "boilerpipe", doc.fields)
        return doc
    }

    private fun extract(page: WebPage): TextDocument? {
        if (page.content == null) {
            LOG.warn("Can not extract page without content, url : " + page.url)
            return null
        }

        try {
            if (page.encoding == null) {
                primerParser.detectEncoding(page)
            }
            val inputSource = page.contentAsSaxInputSource
            val doc = SAXInput().parse(page.location, inputSource)
            val extractor = ChineseNewsExtractor()
            extractor.process(doc)
            return doc
        } catch (e: ProcessingException) {
            LOG.warn("Failed to extract text content by boilerpipe, " + e.message)
        }

        return null
    }

    companion object {
        private val LOG = LogFactory.getLog(BoilerpipeFilter::class.java.name)
    }
}