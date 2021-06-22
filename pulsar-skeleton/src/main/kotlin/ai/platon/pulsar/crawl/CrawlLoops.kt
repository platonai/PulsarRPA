package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.StartStopRunnable

class CrawlLoops(val loops: MutableList<CrawlLoop>) : StartStopRunnable {

    override fun start() {
        loops.forEach { it.start() }
    }

    override fun stop() {
        loops.forEach { it.stop() }
    }
}
