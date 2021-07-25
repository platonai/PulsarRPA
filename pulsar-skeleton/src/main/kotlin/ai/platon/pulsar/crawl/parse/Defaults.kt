package ai.platon.pulsar.crawl.parse

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.html.PrimerParser
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory

class DefaultParser(
    val conf: ImmutableConfig
) : Parser {
    private val logger = LoggerFactory.getLogger(DefaultParser::class.java)
    private val primerParser = PrimerParser(conf)

    override fun parse(page: WebPage): ParseResult {
        val parseContext = primerParser.parseHTMLDocument(page)

        return parseContext.parseResult
    }
}
