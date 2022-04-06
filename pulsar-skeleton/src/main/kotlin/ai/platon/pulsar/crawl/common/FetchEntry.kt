package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.crawl.PulsarEventPipelineHandler
import ai.platon.pulsar.persist.WebPage

class FetchEntry(val page: WebPage, val options: LoadOptions) {

    constructor(url: String, options: LoadOptions, href: String? = null) :
            this(createPageShell(url, options, href), options)

    companion object {

        fun createPageShell(normUrl: NormUrl): WebPage {
            return createPageShell(normUrl.spec, normUrl.options, normUrl.hrefSpec)
        }

        fun createPageShell(url: String, options: LoadOptions, href: String? = null): WebPage {
            val page = WebPage.newWebPage(url, options.conf, href)

            initWebPage(page, options, href)

            return page
        }

        fun initWebPage(page: WebPage, options: LoadOptions, href: String? = null) {
            page.also {
                it.href = href
                it.fetchMode = options.fetchMode
                it.conf = options.conf
                it.args = options.toString()

                it.setVar(PulsarParams.VAR_LOAD_OPTIONS, options)
            }
        }
    }
}
