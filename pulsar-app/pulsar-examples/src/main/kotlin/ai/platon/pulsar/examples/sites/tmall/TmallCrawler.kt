package ai.platon.pulsar.examples.sites.tmall

import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.test.VerboseCrawler

fun main() = withContext { cx ->
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val args = "-i 1s -ii 5m -ol a[href~=item] -ignoreFailure"
    VerboseCrawler(cx).loadOutPages(portalUrl, args)
}
