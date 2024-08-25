package ai.platon.pulsar.skeleton.crawl.common.url

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.common.urls.NormURL
import ai.platon.pulsar.skeleton.crawl.event.WebPageHandler
import java.util.concurrent.TimeUnit

internal class CompleteWebPageHyperlinkHandler(val link: CompletableListenableHyperlink<WebPage>): WebPageHandler() {
    override fun invoke(page: WebPage) {
        link.complete(page)
        link.event.loadEventHandlers.onLoaded.remove(this)

        // TODO: the following code might be better
//        if (link.event.loadEvent.onLoaded.remove(this)) {
//            link.complete(page)
//        }
    }
}

/**
 * Create a completable listenable hyperlink
 * */
fun NormURL.toCompletableListenableHyperlink(): CompletableListenableHyperlink<WebPage> {
    val link = CompletableListenableHyperlink<WebPage>(spec, args = args, href = hrefSpec)

    link.event.loadEventHandlers.onLoaded.addLast(CompleteWebPageHyperlinkHandler(link))
    options.rawEvent?.let { link.event.chain(it) }

    link.completeOnTimeout(WebPage.NIL, options.pageLoadTimeout.seconds + 1, TimeUnit.SECONDS)

    return link
}
