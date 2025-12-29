package ai.platon.pulsar.skeleton.crawl

import ai.platon.pulsar.common.StartStopRunnable
import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware

interface TaskLoop : StartStopRunnable {
    val id: Int
    val name: String
    val config: ImmutableConfig
    val urlFeeder: Iterable<UrlAware>
    val collectors: List<out DataCollector<UrlAware>>
    val taskRunner: TaskRunner

    @Deprecated("Use TaskLoop instead", ReplaceWith("TaskLoop"))
    val crawler: TaskRunner get() = taskRunner
    val display: String
    val abstract: String
    val report: String
}
