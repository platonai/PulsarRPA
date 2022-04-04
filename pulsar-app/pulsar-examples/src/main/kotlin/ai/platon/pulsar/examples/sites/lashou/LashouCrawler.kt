package ai.platon.pulsar.examples.sites.lashou

import ai.platon.pulsar.ql.context.SQLContexts

fun main() {
    val portalUrl = "https://www.lagou.com/wn/jobs?pn=2&fromSearch=true&kd=爬虫"
    // TODO: click is not implemented
    val clickTarget = "#jobList div[class~=p-top] a"
    val args = """-i 1s -ii 30d -click "$clickTarget" -ignoreFailure"""
    SQLContexts.createSession().loadOutPages(portalUrl, args)
}
