package ai.platon.pulsar.examples.sites.jd

import ai.platon.pulsar.examples.common.Crawler
import ai.platon.pulsar.ql.context.withSQLContext

fun main() = withSQLContext { context ->
    Crawler(context).loadOutPages(
        "https://list.jd.com/list.html?cat=652,12345,12349", "-i 1s -ii 5m -ol a[href~=item] -ignoreFailure")
}
