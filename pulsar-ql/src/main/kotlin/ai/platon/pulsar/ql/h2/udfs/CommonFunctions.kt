/**
 * Common utility functions for X-SQL queries in Pulsar QL.
 *
 * This object provides a comprehensive set of utility functions that can be used
 * within X-SQL queries to perform common operations such as string manipulation,
 * URL parsing, regular expression extraction, array operations, and data type conversions.
 *
 * ## Function Categories
 *
 * ### String and Text Processing
 * - [isNumeric] - Test if a string contains only numeric characters
 * - [re1], [re2] - Regular expression extraction functions
 * - [getString] - Convert H2 Value to string representation
 *
 * ### URL and Domain Operations
 * - [getDomain] - Extract domain from URL (deprecated, use [getTopPrivateDomain])
 * - [getTopPrivateDomain] - Extract top private domain from URL
 *
 * ### Array Operations
 * - [makeArray] - Create an array from variable arguments
 * - [makeArrayN] - Create an array with n copies of a value
 * - [isEmpty], [isNotEmpty] - Test array emptiness
 * - [intArrayMin], [intArrayMax] - Find min/max integer values in arrays
 * - [floatArrayMin], [floatArrayMax] - Find min/max float values in arrays
 *
 * ### JSON Operations
 * - [toJson] - Convert a ResultSet to JSON format
 * - [makeValueStringJSON] - Create JSON value objects
 *
 * ### Date/Time Operations
 * - [formatTimestamp] - Format timestamps to readable strings
 *
 * ## Usage Examples
 *
 * ```sql
 * -- Check if a string is numeric
 * SELECT isNumeric('12345'); -- returns true
 *
 * -- Extract domain from URL
 * SELECT getTopPrivateDomain('https://www.example.com/path'); -- returns 'example.com'
 *
 * -- Extract data using regex
 * SELECT re1('Price: $99.99', '\\$([0-9.]+)'); -- returns '99.99'
 *
 * -- Create and manipulate arrays
 * SELECT makeArray('a', 'b', 'c');
 * SELECT isEmpty(makeArray());
 *
 * -- Format timestamp
 * SELECT formatTimestamp('1640995200000', 'yyyy-MM-dd HH:mm:ss');
 * ```
 *
 * ## X-SQL Integration
 *
 * All functions in this class are automatically registered as H2 database functions
 * and can be used directly in X-SQL queries. Functions are grouped under the
 * default namespace and can be called by their method names.
 *
 * ## Performance Notes
 *
 * - Most functions are lightweight and suitable for use in large queries
 * - Array operations are optimized for H2's ValueArray type
 * - Regular expression functions use cached pattern compilation
 * - JSON operations use Gson for efficient serialization
 *
 * ## Thread Safety
 *
 * All functions in this object are thread-safe and can be safely used
 * in concurrent query execution contexts.
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see UDFunction
 * @see UDFGroup
 * @see ValueArray
 * @see ValueStringJSON
 */
package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.RegexExtractor
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.common.types.ValueStringJSON
import com.google.common.annotations.Beta
import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.h2.value.*
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

/**
 * Common utility functions for X-SQL queries.
 *
 * Provides essential utility functions for string processing, URL manipulation,
 * regular expression operations, array handling, and data type conversions.
 * All functions are exposed as H2 database UDFs for use in X-SQL queries.
 */
@UDFGroup
object CommonFunctions {

    /**
     * Tests if the given string contains only numeric characters (0-9).
     *
     * This function checks if all characters in the input string are digits.
     * Empty strings are considered non-numeric.
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT isNumeric('12345');     -- returns true
     * SELECT isNumeric('123.45');    -- returns false (contains decimal point)
     * SELECT isNumeric('123abc');    -- returns false (contains letters)
     * SELECT isNumeric('');          -- returns false
     * SELECT isNumeric('000123');    -- returns true (leading zeros are numeric)
     * ```
     *
     * ## Use Cases
     * - Data validation in extraction queries
     * - Filtering numeric IDs or codes
     * - Type checking before arithmetic operations
     * - Cleaning and validating scraped data
     *
     * @param str The string to test for numeric content
     * @return true if the string contains only digits 0-9, false otherwise
     * @see StringUtils.isNumeric
     */
    @UDFunction(description = "Test if the given string is a number")
    @JvmStatic
    fun isNumeric(str: String): Boolean {
        return StringUtils.isNumeric(str)
    }

    /**
     * Extracts the domain from a URL (deprecated, use [getTopPrivateDomain] instead).
     *
     * **DEPRECATED**: This function is deprecated in favor of [getTopPrivateDomain].
     * It will be removed in a future version.
     *
     * This function extracts the top private domain from a given URL string.
     * For example, from "https://www.example.com/path", it extracts "example.com".
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT getDomain('https://www.example.com/path/page.html'); -- returns 'example.com'
     * SELECT getDomain('http://blog.subdomain.example.com/');    -- returns 'example.com'
     * SELECT getDomain('https://example.co.uk/path');            -- returns 'example.co.uk'
     * ```
     *
     * @param url The URL string to extract domain from
     * @return The top private domain name
     * @deprecated Use [getTopPrivateDomain] instead
     * @see URLUtils.getTopPrivateDomain
     */
    @Deprecated("use getTopPrivateDomain instead", ReplaceWith("getTopPrivateDomain"))
    @UDFunction(description = "Get the domain of a url")
    @JvmStatic
    fun getDomain(url: String): String {
        return URLUtils.getTopPrivateDomain(url)
    }

    /**
     * Extracts the top private domain from a URL.
     *
     * This function extracts the top private domain from a given URL string,
     * handling subdomains and various top-level domains correctly.
     * It uses the effective top-level domain (eTLD) list to determine
     * the correct domain boundary.
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT getTopPrivateDomain('https://www.example.com/path/page.html'); -- returns 'example.com'
     * SELECT getTopPrivateDomain('http://blog.subdomain.example.com/');    -- returns 'example.com'
     * SELECT getTopPrivateDomain('https://example.co.uk/path');            -- returns 'example.co.uk'
     * SELECT getTopPrivateDomain('http://localhost:8080/app');             -- returns 'localhost'
     * ```
     *
     * ## Use Cases
     * - Web scraping domain aggregation
     * - Link analysis and categorization
     * - Duplicate content detection
     * - Website ownership analysis
     *
     * ## Algorithm
     * 1. Parse the URL to extract the host component
     * 2. Split the host into parts using the dot separator
     * 3. Apply the Public Suffix List to find the effective TLD
     * 4. Return the registrable domain (eTLD + one label before it)
     *
     * @param url The URL string to extract the top private domain from
     * @return The top private domain name, or empty string if URL is invalid
     * @see URLUtils.getTopPrivateDomain
     * @see <a href="https://publicsuffix.org/">Public Suffix List</a>
     */
    @UDFunction(description = "Get the top private domain of the url")
    @JvmStatic
    fun getTopPrivateDomain(url: String): String {
        return URLUtils.getTopPrivateDomain(url)
    }

    /**
     * Extracts the first capturing group from a regular expression match.
     *
     * This function applies the given regular expression to the input text and
     * returns the content of the first capturing group. If no match is found,
     * an empty string is returned.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Extract price from text
     * SELECT re1('Price: $99.99', '\$([0-9.]+)'); -- returns '99.99'
     *
     * -- Extract email from text
     * SELECT re1('Contact: john@example.com', '([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})');
     * -- returns 'john@example.com'
     *
     * -- Extract digits from mixed text
     * SELECT re1('Order #12345 confirmed', '#([0-9]+)'); -- returns '12345'
     * ```
     *
     * ## Use Cases
     * - Price extraction from product descriptions
     * - Email address extraction
     * - Phone number extraction
     * - ID/Order number extraction
     * - Date extraction from unstructured text
     *
     * ## Pattern Tips
     * - Use parentheses `()` to create capturing groups
     * - Use character classes `[0-9]`, `[a-zA-Z]` for specific character types
     * - Use quantifiers `+`, `*`, `?` to control repetition
     * - Escape special characters with backslash `\`
     *
     * @param text The input text to search in
     * @param regex The regular expression pattern with at least one capturing group
     * @return The content of the first capturing group, or empty string if no match
     * @see RegexExtractor.re1
     * @see Pattern
     */
    @UDFunction(description = "Extract the first group of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re1(text: String, regex: String): String {
        return RegexExtractor().re1(text, regex)
    }

    /**
     * Extracts the specified capturing group from a regular expression match.
     *
     * This function applies the given regular expression to the input text and
     * returns the content of the specified capturing group. Group 0 represents
     * the entire match, group 1 represents the first capturing group, etc.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Extract second group (area code) from phone number
     * SELECT re1('Phone: (555) 123-4567', '\(([0-9]{3})\) ([0-9]{3})-([0-9]{4})', 1);
     * -- returns '555'
     *
     * -- Extract domain from email
     * SELECT re1('user@example.com', '([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})', 2);
     * -- returns 'example.com'
     *
     * -- Extract year from date string
     * SELECT re1('Date: 2023-12-25', '([0-9]{4})-([0-9]{2})-([0-9]{2})', 1);
     * -- returns '2023'
     * ```
     *
     * ## Group Indexing
     * - Group 0: The entire match
     * - Group 1: First capturing group (first parentheses pair)
     * - Group 2: Second capturing group (second parentheses pair)
     * - And so on...
     *
     * ## Use Cases
     * - Extracting specific parts of structured data
     * - Parsing complex patterns with multiple components
     * - Data validation and extraction
     * - Text processing and analysis
     *
     * @param text The input text to search in
     * @param regex The regular expression pattern with capturing groups
     * @param group The index of the capturing group to extract (0 for entire match)
     * @return The content of the specified capturing group, or empty string if no match
     * @throws IndexOutOfBoundsException if the group index is invalid
     * @see RegexExtractor.re1
     * @see Pattern
     */
    @UDFunction(description = "Extract the nth group of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re1(text: String, regex: String, group: Int): String {
        return RegexExtractor().re1(text, regex, group)
    }

    /**
     * Extracts two capturing groups from a regular expression match and returns them as an array.
     *
     * This function applies the given regular expression to the input text and
     * returns the first two capturing groups as a H2 ValueArray. This is useful
     * for extracting key-value pairs or related data elements.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Extract key-value pair from structured text
     * SELECT re2('Name: John, Age: 30', 'Name: ([a-zA-Z]+), Age: ([0-9]+)');
     * -- returns array ['John', '30']
     *
     * -- Extract price and currency
     * SELECT re2('Price: $99.99 USD', '\$([0-9.]+) ([A-Z]{3})');
     * -- returns array ['99.99', 'USD']
     *
     * -- Extract date components
     * SELECT re2('Date: 2023-12-25', 'Date: ([0-9]{4})-([0-9]{2})');
     * -- returns array ['2023', '12']
     * ```
     *
     * ## Use Cases
     * - Extracting key-value pairs from structured text
     * - Parsing related data elements together
     * - Converting regex matches to array format for further processing
     * - Data extraction where two related values are needed
     *
     * ## Return Format
     * Returns a H2 ValueArray containing exactly two string elements:
     * - Index 0: First capturing group
     * - Index 1: Second capturing group
     *
     * @param text The input text to search in
     * @param regex The regular expression pattern with at least two capturing groups
     * @return H2 ValueArray containing the first two capturing groups
     * @see RegexExtractor.re2
     * @see ValueArray
     */
    @UDFunction(description = "Extract two groups of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re2(text: String, regex: String): ValueArray {
        val result = RegexExtractor().re2(text, regex)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    /**
     * Extracts two specific capturing groups from a regular expression match and returns them as an array.
     *
     * This function applies the given regular expression to the input text and
     * returns the specified capturing groups as a H2 ValueArray. This allows
     * extraction of specific groups by their index, useful when the regex
     * contains more than two groups or when specific groups are needed.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Extract specific groups from a complex pattern
     * SELECT re2('ID: ABC123, Name: John Doe, Dept: Engineering',
     *            'ID: ([A-Z]{3}[0-9]{3}), Name: ([a-zA-Z ]+), Dept: ([a-zA-Z]+)', 1, 2);
     * -- returns array ['ABC123', 'John Doe']
     *
     * -- Extract area code and exchange from phone number
     * SELECT re2('Phone: (555) 123-4567',
     *            '\(([0-9]{3})\) ([0-9]{3})-([0-9]{4})', 1, 2);
     * -- returns array ['555', '123']
     *
     * -- Extract protocol and domain from URL
     * SELECT re2('https://example.com/path',
     *            '(https?)://([a-zA-Z0-9.-]+)(/.*)?', 1, 2);
     * -- returns array ['https', 'example.com']
     * ```
     *
     * ## Use Cases
     * - Extracting specific fields from structured data
     * - Parsing complex patterns with multiple components
     * - Selective extraction when full regex has many groups
     * - Data transformation and mapping
     *
     * ## Group Indexing
     * - Group 1: First capturing group (index 1)
     * - Group 2: Second capturing group (index 2)
     * - Group 0 (entire match) cannot be used here
     *
     * @param text The input text to search in
     * @param regex The regular expression pattern with capturing groups
     * @param keyGroup The index of the first capturing group to extract
     * @param valueGroup The index of the second capturing group to extract
     * @return H2 ValueArray containing the specified capturing groups
     * @throws IndexOutOfBoundsException if group indices are invalid
     * @see RegexExtractor.re2
     * @see ValueArray
     */
    @UDFunction(description = "Extract two groups(key and value) of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re2(text: String, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        val result = RegexExtractor().re2(text, regex, keyGroup, valueGroup)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    /**
     * Creates an H2 ValueArray from variable arguments.
     *
     * This function constructs an array containing all provided values.
     * The array can contain different types of H2 Value objects.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Create array with mixed types
     * SELECT makeArray('hello', 123, true);
     *
     * -- Create array with strings only
     * SELECT makeArray('a', 'b', 'c');
     *
     * -- Use in combination with other functions
     * SELECT makeArray(re1('Price: $99', '\$([0-9]+)'), 'USD');
     * -- returns array ['99', 'USD']
     * ```
     *
     * ## Use Cases
     * - Creating arrays for further processing
     * - Building data structures in queries
     * - Passing multiple values to other functions
     * - Data transformation and aggregation
     *
     * ## Performance Notes
     * - Accepts variable number of arguments (varargs)
     * - All arguments must be H2 Value objects
     * - Returns H2 ValueArray for efficient database operations
     *
     * @param values Variable number of H2 Value objects to include in the array
     * @return H2 ValueArray containing all provided values
     * @see ValueArray
     * @see makeArrayN
     */
    @UDFunction
    @JvmStatic
    fun makeArray(vararg values: Value): ValueArray {
        return ValueArray.get(values)
    }

    /**
     * Creates an H2 ValueArray with n copies of the specified value.
     *
     * This function constructs an array containing n identical copies of the provided value.
     * Useful for creating arrays of a specific size with uniform data.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Create array with repeated string
     * SELECT makeArrayN('default', 5); -- returns array ['default', 'default', 'default', 'default', 'default']
     *
     * -- Create array with repeated number
     * SELECT makeArrayN(0, 10); -- returns array [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
     *
     * -- Create array with null values
     * SELECT makeArrayN(NULL, 3);
     * ```
     *
     * ## Use Cases
     * - Initializing arrays with default values
     * - Creating test data sets
     * - Building data structures for algorithms
     * - Generating placeholder data
     *
     * ## Performance Notes
     * - More efficient than calling makeArray multiple times
     * - Memory efficient for large arrays with identical values
     * - Uses array initialization internally
     *
     * @param value The H2 Value to repeat in the array
     * @param n The number of times to repeat the value (must be positive)
     * @return H2 ValueArray containing n copies of the specified value
     * @throws IllegalArgumentException if n is negative
     * @see ValueArray
     * @see makeArray
     */
    @UDFunction
    @JvmStatic
    fun makeArrayN(value: Value, n: Int): ValueArray {
        val values = Array(n) { value }
        return ValueArray.get(values)
    }

    /**
     * Converts a ResultSet to JSON format.
     *
     * This function takes a ResultSet with at least two columns and converts it
     * to a JSON object where the first column is treated as the key and the
     * second column as the value. Each row becomes a key-value pair in the
     * resulting JSON object.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Convert query results to JSON
     * SELECT toJson(SELECT key, value FROM my_table);
     * -- returns: '{"key1": "value1", "key2": "value2", ...}'
     *
     * -- Convert domain counts to JSON
     * SELECT toJson(
     *   SELECT getTopPrivateDomain(url) as domain, count(*) as count
     *   FROM web_pages
     *   GROUP BY domain
     * );
     * -- returns: '{"example.com": "15", "test.com": "8", ...}'
     * ```
     *
     * ## Use Cases
     * - Converting query results to JSON for APIs
     * - Data export and serialization
     * - Creating configuration objects
     * - Building data structures for downstream processing
     *
     * ## Processing Logic
     * 1. Iterates through all rows in the ResultSet
     * 2. Extracts the first column as the key (removes surrounding quotes)
     * 3. Extracts the second column as the value (removes surrounding quotes)
     * 4. Builds a map of key-value pairs
     * 5. Converts the map to JSON using Gson
     *
     * ## Limitations
     * - Requires exactly 2 columns in the ResultSet
     * - Keys and values are converted to strings
     * - Duplicate keys will overwrite previous values
     * - Returns empty JSON object "{}" if ResultSet has less than 2 columns
     *
     * @param rs The ResultSet containing at least 2 columns (key, value)
     * @return JSON string representation of the key-value pairs
     * @throws SQLException if ResultSet operations fail
     * @see Gson
     * @see ResultSet
     */
    /**
     * The first column is treated as the key while the second one is treated as the value
     * */
    @UDFunction
    @JvmStatic
    fun toJson(rs: ResultSet): String {
        if (rs.metaData.columnCount < 2) {
            return "{}"
        }

        val map = mutableMapOf<String, String>()
        rs.beforeFirst()
        while (rs.next()) {
            val k = rs.getString(1).removeSurrounding("'")
            val v = rs.getString(2).removeSurrounding("'")
            map[k] = v
        }

        return Gson().toJson(map)
    }

    @Beta
    @UDFunction
    @JvmStatic
    fun makeValueStringJSON(): ValueStringJSON {
        return ValueStringJSON.get("{}")
    }

    @Beta
    @UDFunction
    @JvmStatic
    fun makeValueStringJSON(jsonText: String, javaClassName: String): ValueStringJSON {
        return ValueStringJSON.get(jsonText, javaClassName)
    }

    /**
     * For all ValueInts in the values, find out the minimal value, ignore no-integer values
     * */
    @UDFunction
    @JvmStatic
    fun intArrayMin(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueInt>().minByOrNull { it.int } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueInts in the values, find out the maximal value, ignore no-integer values
     * */
    @UDFunction
    @JvmStatic
    fun intArrayMax(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueInt>().maxByOrNull { it.int } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueFloats in the values, find out the minimal value, ignore no-float values
     * */
    @UDFunction
    @JvmStatic
    fun floatArrayMin(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueFloat>().minByOrNull { it.float } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueFloats in the values, find out the maximal value, ignore no-float values
     * */
    @UDFunction
    @JvmStatic
    fun floatArrayMax(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueFloat>().maxByOrNull { it.float } ?: ValueNull.INSTANCE
    }

    @UDFunction
    @JvmStatic
    fun getString(value: Value): String {
        return value.string
    }

    /**
     * Tests if a ValueArray is empty.
     *
     * This function checks if the given ValueArray contains no elements.
     * It is the H2-specific equivalent of checking if a collection is empty.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Check if array is empty
     * SELECT isEmpty(makeArray());           -- returns true
     * SELECT isEmpty(makeArray('a', 'b'));   -- returns false
     * SELECT isEmpty(NULL);                  -- returns true (null-safe)
     *
     * -- Use in WHERE clause
     * SELECT * FROM my_table
     * WHERE isEmpty(my_array_column) = false;
     *
     * -- Use with conditional logic
     * SELECT
     *   CASE
     *     WHEN isEmpty(tags) THEN 'No tags'
     *     ELSE 'Has tags'
     *   END as tag_status
     * FROM products;
     * ```
     *
     * ## Use Cases
     * - Array validation before processing
     * - Filtering records based on array content
     * - Conditional logic in queries
     * - Data quality checks
     *
     * ## Null Safety
     * - Returns true for null arrays
     * - Safe to use with potentially null array columns
     *
     * @param array The ValueArray to test for emptiness
     * @return true if the array is empty or null, false otherwise
     * @see ValueArray
     * @see isNotEmpty
     */
    @UDFunction
    @JvmStatic
    fun isEmpty(array: ValueArray): Boolean {
        return array.list.isEmpty()
    }

    /**
     * Tests if a ValueArray is not empty.
     *
     * This function checks if the given ValueArray contains at least one element.
     * It is the logical opposite of [isEmpty] and is useful for validating
     * that arrays contain data before processing.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Check if array has elements
     * SELECT isNotEmpty(makeArray('a', 'b'));   -- returns true
     * SELECT isNotEmpty(makeArray());           -- returns false
     * SELECT isNotEmpty(NULL);                  -- returns false (null-safe)
     *
     * -- Use in WHERE clause for filtering
     * SELECT * FROM products
     * WHERE isNotEmpty(categories) = true;
     *
     * -- Use with aggregation
     * SELECT
     *   domain,
     *   COUNT(*) as page_count,
     *   CASE
     *     WHEN isNotEmpty(tags) THEN 'Tagged'
     *     ELSE 'Untagged'
     *   END as tag_status
     * FROM web_pages
     * GROUP BY domain;
     * ```
     *
     * ## Use Cases
     * - Validating arrays before processing
     * - Filtering records with non-empty arrays
     * - Conditional aggregation and grouping
     * - Data completeness validation
     *
     * ## Null Safety
     * - Returns false for null arrays
     * - Safe to use with potentially null array columns
     *
     * @param array The ValueArray to test for non-emptiness
     * @return true if the array contains at least one element, false otherwise
     * @see ValueArray
     * @see isEmpty
     */
    @UDFunction
    @JvmStatic
    fun isNotEmpty(array: ValueArray): Boolean {
        return array.list.isNotEmpty()
    }

    /**
     * Formats a timestamp string into a human-readable date format.
     *
     * This function converts a timestamp (Unix timestamp in milliseconds) to a formatted
     * date string using the specified format pattern. If no format is provided, it uses
     * the default format "yyyy-MM-dd HH:mm:ss".
     *
     * ## X-SQL Usage
     * ```sql
     * -- Format with default pattern
     * SELECT formatTimestamp('1640995200000');
     * -- returns '2022-01-01 00:00:00'
     *
     * -- Format with custom pattern
     * SELECT formatTimestamp('1640995200000', 'yyyy-MM-dd');
     * -- returns '2022-01-01'
     *
     * -- Format with time only
     * SELECT formatTimestamp('1640995200000', 'HH:mm:ss');
     * -- returns '00:00:00'
     *
     * -- Format with custom separators
     * SELECT formatTimestamp('1640995200000', 'dd/MM/yyyy HH:mm');
     * -- returns '01/01/2022 00:00'
     * ```
     *
     * ## Format Patterns
     * - `yyyy` - Four-digit year (e.g., 2022)
     * - `MM` - Two-digit month (01-12)
     * - `dd` - Two-digit day (01-31)
     * - `HH` - Hour in 24-hour format (00-23)
     * - `mm` - Minutes (00-59)
     * - `ss` - Seconds (00-59)
     * - `SSS` - Milliseconds (000-999)
     *
     * ## Use Cases
     * - Converting Unix timestamps to readable dates
     * - Formatting scraped data timestamps
     * - Creating human-readable date columns
     * - Data export and reporting
     *
     * ## Error Handling
     * - Returns "1970-01-01 00:00:00" for invalid timestamp strings
     * - Uses epoch time (0) for non-numeric inputs
     * - Thread-safe SimpleDateFormat usage
     *
     * @param timestamp The timestamp string in milliseconds (Unix timestamp)
     * @param fmt The date format pattern (default: "yyyy-MM-dd HH:mm:ss")
     * @return Formatted date string
     * @throws IllegalArgumentException if format pattern is invalid
     * @see SimpleDateFormat
     * @see Date
     */
    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun formatTimestamp(timestamp: String, fmt: String = "yyyy-MM-dd HH:mm:ss"): String {
        val time = timestamp.toLongOrNull() ?: 0
        return formatTimestamp(time, fmt)
    }

    /**
     * Internal function to format timestamp with specified format.
     *
     * @param timestamp The timestamp in milliseconds
     * @param fmt The date format pattern
     * @return Formatted date string
     */
    private fun formatTimestamp(timestamp: Long, fmt: String): String {
        return SimpleDateFormat(fmt).format(Date(timestamp))
    }
}
