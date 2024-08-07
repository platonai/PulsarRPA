package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.urls.UrlUtils

open class NormXSQL(
    val url: String,
    val args: String,
    val sql: String
) {
    val configuredUrl get() = UrlUtils.mergeUrlArgs(url, args)
}

class DegenerateXSQL(uuid: String, sql: String) : NormXSQL("${AppConstants.EXAMPLE_URL}/$uuid", "", sql)
