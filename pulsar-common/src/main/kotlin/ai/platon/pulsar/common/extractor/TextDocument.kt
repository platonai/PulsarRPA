package ai.platon.pulsar.common.extractor

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
     * The title of the content, which is extracted from the text content.
     * */
    var contentTitle: String? = null,
    /**
     * The extracted text content of the document, which is usually with links, ads and other irrelevant contents removed.
     * */
    var textContent: String? = null,
    /**
     * The extracted fields.
     * */
    var additionalFields: Map<String, String>? = null
)
