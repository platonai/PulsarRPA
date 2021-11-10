package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.examples.common.Crawler

fun main() = withContext { context ->
    repeat(2) {
        // Crawler(context).load("https://item.jd.com/100010209184.html", "-i 1s")
        Crawler(context).loadOutPages(
            "https://list.jd.com/list.html?cat=652,12345,12349", "-i 1s -ii 5m -ol a[href~=item] -ignoreFailure")
    }
}
