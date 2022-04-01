package ai.platon.pulsar.examples.sites.lashou

import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseCrawler

fun main() = withSQLContext { context ->
    val clickTarget = "#jobList div[class~=p-top] a"
    VerboseCrawler(context).loadOutPages(
        "https://www.lagou.com/wn/jobs?pn=2&fromSearch=true&kd=爬虫",
        "-i 1s -ii 30d -click \"$clickTarget\" -ignoreFailure"
    )
}
