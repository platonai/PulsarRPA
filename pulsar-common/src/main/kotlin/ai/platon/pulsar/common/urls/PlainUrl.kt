package ai.platon.pulsar.common.urls

open class PlainUrl(
    url: String,
    args: String? = null,
    referer: String? = null
) : AbstractUrl(url, args, referer)
