package org.warps.pulsar.examples

import org.warps.pulsar.Pulsar
import org.warps.pulsar.common.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import org.warps.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION
import org.warps.pulsar.common.options.LoadOptions
import org.warps.pulsar.persist.WebPageFormatter

object WebAccess {
    val pulsar = Pulsar()

    fun load() {
        val url = "http://list.mogujie.com/book/jiadian/10059513"
        val page = pulsar.load("$url --parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome")
        println(WebPageFormatter(page).withLinks())

        val document = pulsar.parse(page)
        val title = document.selectFirst(".goods_item .title").text()
        println(title)
    }

    fun loadAll() {
        val url = "http://www.sh.chinanews.com/jinrong/index.shtml"
        val portal = pulsar.load("$url --parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome")
        val pages = pulsar.parallelLoadAll(portal.simpleLiveLinks.filter { it.contains("jinrong") }, LoadOptions.parse("--parse"))
        pages.forEach { println("${it.url} ${it.contentTitle}") }
    }
}

fun main(args: Array<String>) {
    // WebAccess.load()
    WebAccess.loadAll()
}
