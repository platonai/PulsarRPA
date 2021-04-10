package ai.platon.pulsar.client.examples.v1

import ai.platon.pulsar.client.v1.Scraper
import kotlin.system.exitProcess

fun main() {
    val host = "crawl0"
    val authToken = "rhlwTRBk-1-de14124c7ace3d93e38a705bae30376c"

    val scraper = Scraper(host, authToken)
    val uuid = "dae68085-f1eb-47f7-8907-8980cfa43b0f"
    scraper.await(uuid)

    exitProcess(0)
}
