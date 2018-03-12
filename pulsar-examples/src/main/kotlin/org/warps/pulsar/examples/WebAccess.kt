package org.warps.pulsar.examples

import org.warps.pulsar.Pulsar
import org.warps.pulsar.common.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import org.warps.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION
import org.warps.pulsar.persist.WebPageFormatter

fun main(args: Array<String>) {
    val pulsar = Pulsar()
    val url = "http://list.mogujie.com/book/jiadian/10059513"
    val page = pulsar.load("$url --parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome")
    println(WebPageFormatter(page).withLinks())

    val document = pulsar.parse(page)
    val title = document.selectFirst(".goods_item .title").text()
    println(title)
}
