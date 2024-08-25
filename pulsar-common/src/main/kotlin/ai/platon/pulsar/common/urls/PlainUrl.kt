package ai.platon.pulsar.common.urls

open class PlainUrl(
    url: String,
    args: String? = null,
    referrer: String? = null
) : AbstractUrl(url, args = args, referrer = referrer)
