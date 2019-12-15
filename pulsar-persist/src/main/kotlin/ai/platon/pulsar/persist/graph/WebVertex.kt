package ai.platon.pulsar.persist.graph

import ai.platon.pulsar.persist.WebPage

/**
 * Created by vincent on 16-12-29.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class WebVertex(val url: String, var page: WebPage? = null) {

    constructor(page: WebPage): this(page.url, page)

    fun hasWebPage(): Boolean {
        return page != null
    }

    override fun equals(other: Any?): Boolean {
        return other is WebVertex && other.url == url
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    override fun toString(): String {
        return url
    }
}
