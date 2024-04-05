package ai.platon.pulsar.common.extractor

import java.time.Instant

/**
 * Represents a text document extracted from a text page, such as a news article.
 * */
open class TextDocument(
    /**
     * The url of the document.
     * */
    val url: String,
    /**
     * The title of the document, which is in <title> tag.
     * */
    var pageTitle: String? = null,
    /**
     * The title of the content, which extracted from the text content.
     * */
    var contentTitle: String? = null,
    /**
     * The extracted text content of the document, which is usually with links, ads and other irrelevant contents removed.
     * */
    var textContent: String? = null,
    /**
     * The extracted fields.
     * */
    var fields: MutableMap<String, String> = mutableMapOf(),
    /**
     * The publishing time of the document.
     * */
    var publishTime: Instant? = null,
    /**
     * The modified time of the document.
     * */
    var modifiedTime: Instant? = null,
)
