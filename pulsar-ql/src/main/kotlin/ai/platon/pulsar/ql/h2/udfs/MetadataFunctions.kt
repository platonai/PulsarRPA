/**
 * Metadata and page information functions for X-SQL queries in Pulsar QL.
 *
 * This object provides functions for accessing and formatting web page metadata
 * stored in the database. It enables retrieval of page information, formatting,
 * and metadata extraction from cached web pages.
 *
 * ## Function Categories
 *
 * ### Page Retrieval and Formatting
 * - [get] - Load and format web page metadata as string
 *
 * ## Usage Examples
 *
 * ```sql
 * -- Get formatted page information
 * SELECT META.get('https://example.com');
 *
 * -- Use in data extraction queries
 * SELECT
 *   url,
 *   META.get(url) as page_info
 * FROM (
 *   SELECT 'https://example.com' as url
 * ) t;
 * ```
 *
 * ## X-SQL Integration
 *
 * All metadata functions are automatically registered as H2 database functions under the
 * "META" namespace. They can be used directly in X-SQL queries for accessing page metadata.
 *
 * ## Performance Notes
 *
 * - Functions load pages from database cache when available
 * - Respects session-level caching configuration
 * - Formatting is done on-demand
 * - Minimal memory overhead for metadata operations
 *
 * ## Thread Safety
 *
 * All functions in this object are thread-safe and can be safely used
 * in concurrent query execution contexts.
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see WebPageFormatter
 * @see UDFGroup
 * @see UDFunction
 * @see H2SessionFactory
 */
package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.persist.model.WebPageFormatter
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import ai.platon.pulsar.ql.common.annotation.H2Context
import java.sql.Connection

/**
 * Metadata functions for X-SQL queries.
 *
 * Provides functions for accessing and formatting web page metadata and information
 * from the database cache within X-SQL queries.
 */
@UDFGroup(namespace = "META")
object MetadataFunctions {

    /**
     * Loads a web page from the database and returns formatted page information.
     *
     * This function retrieves a web page from the database cache (loading it if necessary)
     * and returns a formatted string representation containing page metadata such as
     * URL, title, content metadata, and other page information.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Get page information
     * SELECT META.get('https://example.com'); -- returns formatted page info
     *
     * -- Use in extraction queries
     * SELECT
     *   url,
     *   META.get(url) as page_metadata
     * FROM my_urls;
     *
     * -- Combine with other functions
     * SELECT
     *   DOM.text(DOM.selectFirst(DOM.load(url), 'title')) as title,
     *   META.get(url) as metadata
     * FROM (
     *   SELECT 'https://example.com' as url
     * ) t;
     * ```
     *
     * ## Use Cases
     * - Retrieving page metadata and information
     * - Debugging page loading issues
     * - Extracting page properties for analysis
     * - Combining with DOM extraction functions
     *
     * ## Output Format
     * The returned string contains formatted page information including:
     * - Page URL and basic properties
     * - Content metadata and headers
     * - Crawl and fetch information
     * - Page status and configuration
     *
     * ## Performance Notes
     * - Loads from cache when available
     * - Respects session cache policies
     * - Uses WebPageFormatter for consistent output
     * - Minimal overhead for metadata retrieval
     *
     * @param conn The H2 database connection context
     * @param url The URL of the page to retrieve and format
     * @return Formatted string containing page metadata and information
     * @see WebPageFormatter
     * @see H2SessionFactory
     */
    @UDFunction(description = "Get a page specified by url from the database, return the formatted page as a string")
    @JvmStatic
    fun get(@H2Context conn: Connection, url: String): String {
        val page = H2SessionFactory.getSession(conn).load(url)
        return WebPageFormatter(page).toString()
    }
}
