package ai.platon.pulsar.common.urls

/**
 * A hyperlink datum is a data class that represents a hyperlink.
 * */
data class HyperlinkDatum(
    /**
     * The url specification of the hyperlink, it is usually normalized, and can contain load arguments.
     * */
    var url: String,
    /**
     * A hyperlink should have a text, so the default value is an empty string
     * */
    var text: String = "",
    /**
     * The link order, e.g., the order in which the link appears on the referrer page.
     * */
    var order: Int = 0,
    /**
     * A hyperlink might have a referrer, so the default value is null
     * */
    var referrer: String? = null,
    /**
     * The load argument, can be parsed into a LoadOptions
     * */
    var args: String? = null,
    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    var href: String? = null,
    /**
     * If this link is persistable
     * */
    var isPersistable: Boolean = true,
    /**
     * The priority of this hyperlink
     * */
    var priority: Int = 0,
    /**
     * The language of this hyperlink
     * */
    var lang: String = "*",
    /**
     * The country of this hyperlink
     * */
    var country: String = "*",
    /**
     * The district of this hyperlink
     * */
    var district: String = "*",
    /**
     * The maximum number of retries
     * */
    var nMaxRetry: Int = 3,
    /**
     * The depth
     * */
    var depth: Int = 0,
)
