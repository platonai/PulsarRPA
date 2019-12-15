package ai.platon.pulsar.crawl.parse.html

import ai.platon.pulsar.crawl.parse.ParseResult
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import org.w3c.dom.DocumentFragment

/**
 * Created by vincent on 17-7-28.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class ParseContext(
        val page: WebPage,
        val parseResult: ParseResult,
        val metaTags: HTMLMetaTags? = null,
        // deprecated, may not support in further version. Use jsoup instead
        val documentFragment: DocumentFragment? = null,
        val document: FeaturedDocument? = null
)
