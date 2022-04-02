package ai.platon.pulsar.examples.sites.lashou

import ai.platon.pulsar.ql.context.SQLContexts

fun main() {
    val session = SQLContexts.createSession()
    // TODO: click is not implemented
    val clickTarget = "#jobList div[class~=p-top] a"
    session.loadOutPages(
        "https://www.lagou.com/wn/jobs?pn=2&fromSearch=true&kd=爬虫",
        "-i 1s -ii 30d -click \"$clickTarget\" -ignoreFailure"
    )
}
