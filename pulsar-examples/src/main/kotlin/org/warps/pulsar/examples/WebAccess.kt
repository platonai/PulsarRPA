package org.warps.pulsar.examples

import org.warps.pulsar.Pulsar
import org.warps.pulsar.common.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION
import org.warps.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION
import org.warps.pulsar.persist.WebPageFormatter

fun main(args: Array<String>) {
    val pulsar = Pulsar()
    val portalUrl = "http://list.mogujie.com/book/jiadian/10059513"
    val portalPage = pulsar.load("$portalUrl --parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium")
    println(WebPageFormatter(portalPage).withLinks())
}
