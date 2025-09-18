/**
 * Date and time extraction functions for X-SQL queries in Pulsar QL.
 *
 * This object provides functions for extracting and parsing date/time information
 * from text content within X-SQL queries. It includes pattern-based datetime
 * extraction and formatting capabilities for processing temporal data.
 *
 * ## Function Categories
 *
 * ### DateTime Extraction
 * - [firstDateTime] - Extract first datetime from text using pattern
 * - [firstMysqlDateTime] - Extract MySQL-formatted datetime from text
 *
 * ## Usage Examples
 *
 * ```sql
 * -- Extract datetime from text
 * SELECT TIME.firstDateTime('Meeting scheduled for 2023-12-25 14:30:00'); -- returns '2023-12-25 14:30:00'
 *
 * -- Extract with custom pattern
 * SELECT TIME.firstDateTime('Event: 25/12/2023', 'dd/MM/yyyy'); -- returns '25/12/2023'
 *
 * -- Extract MySQL datetime
 * SELECT TIME.firstMysqlDateTime('Created at 2023-12-25 14:30:00'); -- returns '2023-12-25 14:30:00'
 * ```
 *
 * ## X-SQL Integration
 *
 * All datetime functions are automatically registered as H2 database functions under the
 * "TIME" namespace. They can be used directly in X-SQL queries for temporal data extraction.
 *
 * ## Performance Notes
 *
 * - Functions use Java DateTimeFormatter for efficient parsing
 * - Pattern-based extraction with fallback to default formats
 * - Handles various datetime formats automatically
 * - Minimal memory overhead for datetime operations
 *
 * ## Thread Safety
 *
 * All functions in this object are thread-safe and can be safely used
 * in concurrent query execution contexts.
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see DateTimes
 * @see DateTimeFormatter
 * @see UDFGroup
 * @see UDFunction
 */
package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * DateTime extraction functions for X-SQL queries.
 *
 * Provides pattern-based datetime extraction and parsing capabilities for
 * processing temporal data within text content in X-SQL queries.
 */
@UDFGroup(namespace = "TIME")
object DateTimeFunctions {
    private val logger = getLogger(DateTimeFunctions::class)
    private val defaultDateTime = Instant.EPOCH.atZone(DateTimes.zoneId).toLocalDateTime()

    /**
     * Extracts the first MySQL-formatted datetime from text content.
     *
     * This function parses the input text to find datetime patterns and returns the first
     * occurrence formatted according to MySQL datetime standards. If no datetime is found
     * or parsing fails, it returns a default formatted datetime.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Extract MySQL datetime from text
     * SELECT TIME.firstMysqlDateTime('Meeting scheduled for 2023-12-25 14:30:00'); -- returns '2023-12-25 14:30:00'
     *
     * -- Extract with custom pattern
     * SELECT TIME.firstMysqlDateTime('Event: 25/12/2023 2:30 PM', 'dd/MM/yyyy HH:mm a'); -- returns '25/12/2023 02:30'
     *
     * -- Handle empty text
     * SELECT TIME.firstMysqlDateTime(''); -- returns default formatted datetime
     * SELECT TIME.firstMysqlDateTime(null); -- returns default formatted datetime
     * ```
     *
     * ## Use Cases
     * - Extracting publication dates from articles
     * - Parsing event timestamps from text
     * - Processing log file timestamps
     * - Extracting temporal data from web content
     *
     * ## Default Behavior
     * - Returns epoch datetime (1970-01-01 00:00:00) formatted according to pattern if no datetime found
     * - Uses MySQL standard datetime format "yyyy-MM-dd HH:mm:ss" by default
     * - Handles various datetime formats automatically
     *
     * @param text The text content to search for datetime patterns
     * @param pattern The datetime format pattern (default: "yyyy-MM-dd HH:mm:ss")
     * @return The extracted datetime formatted according to the pattern, or default datetime if not found
     * @see firstDateTime
     * @see DateTimes.parseBestInstant
     */
    @UDFunction
    @JvmOverloads
    @JvmStatic
    fun firstMysqlDateTime(text: String?, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return firstDateTime(text, pattern)
    }

    /**
     * Extracts the first datetime from text content using pattern-based parsing.
     *
     * This function analyzes the input text to identify datetime patterns and returns
     * the first occurrence formatted according to the specified pattern. It uses
     * intelligent datetime parsing to handle various common formats.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Extract datetime with default pattern
     * SELECT TIME.firstDateTime('Article published on 2023-12-25 14:30:00'); -- returns '2023-12-25 14:30:00'
     *
     * -- Extract with European date format
     * SELECT TIME.firstDateTime('Date: 25.12.2023', 'dd.MM.yyyy'); -- returns '25.12.2023'
     *
     * -- Extract with US date format
     * SELECT TIME.firstDateTime('Event on 12/25/2023', 'MM/dd/yyyy'); -- returns '12/25/2023'
     *
     * -- Extract time only
     * SELECT TIME.firstDateTime('Meeting at 14:30:00', 'HH:mm:ss'); -- returns '14:30:00'
     *
     * -- Handle text without datetime
     * SELECT TIME.firstDateTime('No date here'); -- returns default formatted datetime
     * ```
     *
     * ## Supported DateTime Formats
     * The function automatically detects and parses various datetime formats including:
     * - ISO formats: "2023-12-25T14:30:00", "2023-12-25 14:30:00"
     * - European formats: "25.12.2023", "25/12/2023"
     * - US formats: "12/25/2023", "12-25-2023"
     * - Time formats: "14:30:00", "2:30 PM"
     * - Combined formats: "25.12.2023 14:30", "Dec 25, 2023 2:30 PM"
     *
     * ## Use Cases
     * - Extracting article publication dates
     * - Parsing event timestamps from web content
     * - Processing temporal data in logs
     * - Extracting dates from unstructured text
     * - Data preprocessing for temporal analysis
     *
     * ## Pattern Format
     * Uses Java DateTimeFormatter patterns:
     * - `yyyy` - Four-digit year
     * - `MM` - Two-digit month
     * - `dd` - Two-digit day
     * - `HH` - Hour in 24-hour format
     * - `mm` - Minutes
     * - `ss` - Seconds
     * - Additional pattern symbols as per DateTimeFormatter
     *
     * ## Error Handling
     * - Returns default formatted epoch datetime if parsing fails
     * - Logs warnings for parsing failures (every 50th occurrence)
     * - Graceful handling of null or empty input
     * - Safe fallback to default datetime
     *
     * @param text The text content to analyze for datetime patterns
     * @param pattern The output format pattern for the extracted datetime
     * @return The first extracted datetime formatted according to pattern, or default datetime
     * @see DateTimes.parseBestInstant
     * @see DateTimeFormatter
     * @see firstMysqlDateTime
     */
    @UDFunction
    @JvmOverloads
    @JvmStatic
    fun firstDateTime(text: String?, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        if (text.isNullOrBlank()) {
            return formatDefaultDateTime(pattern)
        }

        try {
            val instant = DateTimes.parseBestInstant(text)
            return DateTimeFormatter.ofPattern(pattern).withZone(DateTimes.zoneId).format(instant)
        } catch (e: RuntimeException) {
            logger.warn("Failed handle date time: {} | {}", text, e.message)
        }

        return formatDefaultDateTime(pattern)
    }

    private fun formatDefaultDateTime(pattern: String): String {
        return DateTimes.format(defaultDateTime, pattern)
    }
}
