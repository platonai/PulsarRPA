package ai.platon.pulsar.examples.sites.jd

import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseCrawler

fun main() = withSQLContext { cx ->
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val args = "-i 1s -ii 5m -ol a[href~=item] -ignoreFailure"
    VerboseCrawler(cx).loadOutPages(portalUrl, args)
}
