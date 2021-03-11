package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.persist.WebPage

class AddRefererAfterFetchHandler(val url: UrlAware): WebPageHandler() {
    override fun invoke(page: WebPage) { url.referer?.let { page.referrer = it } }
}
