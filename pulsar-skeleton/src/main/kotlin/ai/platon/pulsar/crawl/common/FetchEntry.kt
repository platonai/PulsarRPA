package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormURL
import ai.platon.pulsar.persist.WebPage

class FetchEntry(val page: WebPage, val options: LoadOptions) {

    constructor(url: String, options: LoadOptions, href: String? = null) :
            this(createPageShell(url, options, href), options)

    companion object {

        fun createPageShell(normURL: NormURL): WebPage {
            val referer = normURL.detail?.referrer ?: normURL.options.referrer
            return createPageShell(normURL.spec, normURL.options, normURL.hrefSpec, referer)
        }

        fun createPageShell(url: String, options: LoadOptions, href: String? = null, referrer: String? = null): WebPage {
            val page = WebPage.newWebPage(url, options.conf, href)
            initWebPage(page, options, href, referrer)
            return page
        }

        fun initWebPage(page: WebPage, options: LoadOptions, href: String? = null, referrer: String? = null) {
            page.also {
                it.href = href
                it.fetchMode = options.fetchMode
                it.conf = options.conf
                it.args = options.toString()
                it.maxRetries = options.nMaxRetry
                it.isResource = options.isResource
                it.referrer = referrer

                // since LoadOptions is not visible by WebPage, we use an unsafe method to pass the load options
                it.setVar(PulsarParams.VAR_LOAD_OPTIONS, options)
            }
        }
    }
}
