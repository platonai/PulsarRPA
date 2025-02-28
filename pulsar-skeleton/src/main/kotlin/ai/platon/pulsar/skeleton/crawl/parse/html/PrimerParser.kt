package ai.platon.pulsar.skeleton.crawl.parse.html

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.common.EncodingDetector
import ai.platon.pulsar.skeleton.crawl.parse.Parser
import org.slf4j.LoggerFactory
import java.util.*

/**
 * A very simple DOM parser.
 */
class PrimerParser(val conf: ImmutableConfig) {
    private val logger = LoggerFactory.getLogger(Parser::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    
    private var encodingDetector = EncodingDetector(conf)

    fun detectEncoding(page: WebPage) {
        val encoding = encodingDetector.sniffEncoding(page)
        if (encoding != null && encoding.isNotEmpty()) {
            page.encoding = encoding
        } else {
            logger.warn("Failed to detect encoding, url: " + page.url)
        }
    }
    
    @Throws(Exception::class)
    fun parseHTMLDocument(page: WebPage): ParseContext {
        tracer?.trace(
            "{}.\tParsing page | {} | {} | {} | {}",
            page.id, Strings.compactFormat(page.contentLength),
            page.protocolStatus, page.htmlIntegrity, page.url
        )
        
        if (page.encoding == null) {
            detectEncoding(page)
        }
        
        val jsoupParser = JsoupParser(page, conf)
        jsoupParser.parse()
        
        return ParseContext(page, jsoupParser.document)
    }
}
