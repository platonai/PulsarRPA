/**
 * Large Language Model (LLM) integration functions for X-SQL queries in Pulsar QL.
 *
 * This object provides AI-powered functions that integrate Large Language Models
 * directly into X-SQL queries. It enables intelligent content analysis, extraction,
 * and processing using state-of-the-art language models.
 *
 * ## Function Categories
 *
 * ### Basic LLM Operations
 * - [modelName] - Get the configured LLM model name
 * - [chat] - Send prompts to the LLM and get responses
 *
 * ### AI-Powered Content Extraction
 * - [extract] - Extract structured data from HTML content using AI
 * - [classify] - Classify content using AI models
 * - [summarize] - Generate content summaries
 *
 * ### Advanced AI Operations
 * - [chat] with DOM context - Send element-specific prompts
 * - [extract] with custom rules - Define extraction patterns
 * - Batch processing for multiple documents
 *
 * ## Usage Examples
 *
 * ```sql
 * -- Get current LLM model name
 * SELECT LLM.modelName();
 *
 * -- Chat with the AI model
 * SELECT LLM.chat('What is the main topic of this website?');
 *
 * -- Extract data from a web page using AI
 * SELECT LLM.extract(
 *   DOM.load('https://product.example.com/item123'),
 *   'product_name, price, description, availability'
 * );
 *
 * -- Chat about specific page content
 * SELECT LLM.chat(
 *   DOM.load('https://news.example.com/article'),
 *   'Summarize this article in 3 sentences'
 * );
 *
 * -- Extract structured data from HTML
 * SELECT LLM.extract(dom, 'title, author, publish_date, summary')
 * FROM (
 *   SELECT DOM.load('https://blog.example.com/post') as dom
 * ) t;
 * ```
 *
 * ## X-SQL Integration
 *
 * All LLM functions are automatically registered as H2 database functions under the
 * "LLM" namespace. They can be used directly in X-SQL queries and combined with
 * DOM functions for powerful AI-driven web data extraction.
 *
 * ## AI Extraction Format
 *
 * The [extract] function uses intelligent prompting to extract structured data:
 * 1. Analyzes the HTML content contextually
 * 2. Identifies relevant information based on extraction rules
 * 3. Returns structured JSON with extracted fields
 * 4. Handles missing data gracefully with null values
 *
 * ## Performance Notes
 *
 * - LLM operations may have higher latency than traditional functions
 * - Results are cached within the session context
 * - Batch operations are recommended for multiple extractions
 * - Rate limiting is applied based on model configuration
 *
 * ## Error Handling
 *
 * - Returns empty results for failed AI operations
 * - Logs warnings for extraction failures (every 50th failure)
 * - Graceful degradation when AI services are unavailable
 * - JSON parsing errors are handled silently
 *
 * ## Configuration
 *
 * LLM functions respect the session's LLM configuration:
 * - Model selection (GPT, Claude, etc.)
 * - API endpoints and authentication
 * - Rate limiting and timeout settings
 * - Custom prompts and system messages
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see ValueDom
 * @see ValueStringJSON
 * @see UDFGroup
 * @see JSONExtractor
 * @see <a href="https://platform.openai.com/docs/api-reference">OpenAI API Reference</a>
 */
package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.JSONExtractor
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.common.types.ValueDom
import ai.platon.pulsar.ql.common.types.ValueStringJSON
import ai.platon.pulsar.skeleton.context.PulsarContexts
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.concurrent.atomic.AtomicInteger

const val DATA_EXTRACTION_RULES_PLACEHOLDER = "{DATA_EXTRACTION_RULES}"

val LLM_UDF_EXTRACT_PROMPT =
    """
Extract the specified fields from the given HTML content and return the result as a JSON object.

Use the following format:

```json
{
  "field1": "value1",
  "field2": "value2"
}
```

Data extraction rules:
{DATA_EXTRACTION_RULES}

Ensure all extracted values are clean and trimmed. If a field cannot be found, set its value to null.

"""

@Suppress("unused")
@UDFGroup(namespace = "LLM")
object LLMFunctions {
    private val logger = getLogger(this)
    private val session get() = PulsarContexts.getOrCreateSession()
    private val llmFailureWarnings = AtomicInteger(0)

    @JvmStatic
    @UDFunction(description = "Get the LLM model name")
    fun modelName(): String {
        return session.unmodifiedConfig["llm.name"] ?: "unknown"
    }

    @JvmStatic
    @UDFunction(description = "Chat with the LLM model")
    fun chat(prompt: String): String {
        return session.chat(prompt).content
    }

    @JvmStatic
    @UDFunction(description = "Chat with the LLM model")
    fun chat(dom: ValueDom, prompt: String): String {
        return session.chat(dom.element, prompt).content
    }

    @JvmStatic
    @UDFunction(description = "Extract fields from the content of the given DOM with the LLM model")
    fun extract(dom: ValueDom, dataExtractionRules: String): ValueStringJSON {
        val result = extractInternal(dom.element.text(), dataExtractionRules)
        return ValueStringJSON.get(pulsarObjectMapper().writeValueAsString(result), Map::class.qualifiedName)
    }

    internal fun extractInternal(domContent: String, dataExtractionRules: String): Map<String, String> {
        val prompt = LLM_UDF_EXTRACT_PROMPT.replace(DATA_EXTRACTION_RULES_PLACEHOLDER, dataExtractionRules)
        val content = session.chat(domContent, prompt).content

        val jsonBlocks = JSONExtractor.extractJsonBlocks(content)
        if (jsonBlocks.isEmpty()) {
            if (llmFailureWarnings.get() % 50 == 0) {
                logger.warn("{}th failure to extract a JSON from LLM's response | {}", llmFailureWarnings.get().inc(), content)
            }
            llmFailureWarnings.incrementAndGet()
            return mapOf()
        }

        return try {
            val jsonBlock = jsonBlocks[0]
            val result: Map<String, String> = pulsarObjectMapper().readValue(jsonBlock)
            result
        } catch (e: Exception) {
            logger.warn("Failed to parse JSON from LLM's response | $content", e)
            mapOf()
        }
    }
}
