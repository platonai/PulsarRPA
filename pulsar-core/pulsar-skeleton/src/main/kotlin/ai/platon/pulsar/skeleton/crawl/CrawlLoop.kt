package ai.platon.pulsar.skeleton.crawl

import ai.platon.pulsar.common.StartStopRunnable
import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware

interface CrawlLoop: StartStopRunnable {
    val id: Int
    val name: String
    val config: ImmutableConfig
    val urlFeeder: Iterable<UrlAware>
    val collectors: List<out DataCollector<UrlAware>>
    val crawler: Crawler
    val display: String
    val abstract: String
    val report: String
}
