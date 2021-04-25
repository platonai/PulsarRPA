package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.urls.Urls

open class ScrapingSQL(
    val url: String,
    val args: String,
    val sql: String
) {
    val configuredUrl get() = Urls.mergeUrlArgs(url, args)
}
