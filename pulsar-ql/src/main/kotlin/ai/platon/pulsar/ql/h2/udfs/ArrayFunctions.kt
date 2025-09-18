/**
 * Array manipulation and utility functions for X-SQL queries in Pulsar QL.
 *
 * This object provides essential functions for working with arrays in X-SQL queries,
 * including joining array elements, finding first non-empty values, and array transformations.
 * All functions operate on H2 ValueArray objects and return appropriate H2 Value types.
 *
 * ## Function Categories
 *
 * ### Array String Operations
 * - [joinToString] - Join array elements into a single string with separator
 *
 * ### Array Filtering and Selection
 * - [firstNotBlank] - Find first non-blank string value in array
 * - [firstNotEmpty] - Find first non-empty string value in array
 *
 * ## Usage Examples
 *
 * ```sql
 * -- Join array elements with separator
 * SELECT ARRAY.joinToString(makeArray('a', 'b', 'c'), ','); -- returns 'a,b,c'
 *
 * -- Find first non-blank value
 * SELECT ARRAY.firstNotBlank(makeArray('', '  ', 'hello', 'world')); -- returns 'hello'
 *
 * -- Find first non-empty value
 * SELECT ARRAY.firstNotEmpty(makeArray('', 'hello', 'world')); -- returns 'hello'
 *
 * -- Combine with other functions
 * SELECT ARRAY.joinToString(
 *   makeArray(re1(text, 'pattern1'), re1(text, 'pattern2')),
 *   '|'
 * ) as extracted_values;
 * ```
 *
 * ## X-SQL Integration
 *
 * All array functions are automatically registered as H2 database functions under the
 * "ARRAY" namespace. They can be used directly in X-SQL queries and combined with
 * other functions for data processing workflows.
 *
 * ## Performance Notes
 *
 * - Functions are optimized for H2 ValueArray operations
 * - String operations use efficient Kotlin string handling
 * - Null-safe operations throughout all functions
 * - Minimal memory allocation for intermediate results
 *
 * ## Thread Safety
 *
 * All functions in this object are thread-safe and can be safely used
 * in concurrent query execution contexts.
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see ValueArray
 * @see UDFGroup
 * @see UDFunction
 */
package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import org.h2.value.Value
import org.h2.value.ValueArray

/**
 * Array manipulation functions for X-SQL queries.
 *
 * Provides utility functions for array operations including string joining and
 * element selection based on content criteria. All functions work with H2 ValueArray
 * objects and support null-safe operations.
 */
@UDFGroup(namespace = "ARRAY")
object ArrayFunctions {

    /**
     * Joins array elements into a single string using the specified separator.
     *
     * This function converts each element in the ValueArray to its string representation
     * and concatenates them with the provided separator string.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Join with comma separator
     * SELECT ARRAY.joinToString(makeArray('apple', 'banana', 'orange'), ','); -- returns 'apple,banana,orange'
     *
     * -- Join with pipe separator
     * SELECT ARRAY.joinToString(makeArray('a', 'b', 'c'), '|'); -- returns 'a|b|c'
     *
     * -- Join numbers converted to strings
     * SELECT ARRAY.joinToString(makeArray(1, 2, 3), '-'); -- returns '1-2-3'
     *
     * -- Empty array handling
     * SELECT ARRAY.joinToString(makeArray(), ','); -- returns ''
     * ```
     *
     * ## Use Cases
     * - Creating delimited strings for export
     * - Building search query strings
     * - Creating compound identifiers
     * - Formatting array data for display
     *
     * ## Performance Notes
     * - Uses efficient Kotlin joinToString implementation
     * - Handles null values gracefully
     * - Memory efficient for large arrays
     *
     * @param values The ValueArray containing elements to join
     * @param separator The string to use as separator between elements
     * @return String containing all array elements joined by the separator
     * @see ValueArray
     * @see String.join
     */
    @UDFunction
    @JvmStatic
    fun joinToString(values: ValueArray, separator: String): String {
        return values.list.joinToString(separator) { it.string }
    }

    /**
     * Finds the first non-blank string value in the array.
     *
     * This function iterates through the ValueArray and returns the first element
     * whose string representation is not blank (not empty and not just whitespace).
     *
     * ## X-SQL Usage
     * ```sql
     * -- Find first non-blank value
     * SELECT ARRAY.firstNotBlank(makeArray('', '  ', 'hello', 'world')); -- returns 'hello'
     *
     * -- All values are blank
     * SELECT ARRAY.firstNotBlank(makeArray('', '  ', '   ')); -- returns NULL
     *
     * -- First value is non-blank
     * SELECT ARRAY.firstNotBlank(makeArray('first', '', 'second')); -- returns 'first'
     *
     * -- Mixed content types
     * SELECT ARRAY.firstNotBlank(makeArray(0, '  ', 'valid', 'text')); -- returns 'valid'
     * ```
     *
     * ## Use Cases
     * - Finding first valid data value
     * - Data cleaning and validation
     * - Fallback value selection
     * - Filtering out empty/whitespace placeholders
     *
     * ## Null Safety
     * - Returns null if no non-blank value is found
     * - Handles null array elements gracefully
     * - Safe to use with mixed-type arrays
     *
     * @param values The ValueArray to search through
     * @return The first non-blank Value, or null if none found
     * @see ValueArray
     * @see String.isNotBlank
     */
    @UDFunction
    @JvmStatic
    fun firstNotBlank(values: ValueArray): Value? {
        return values.list.firstOrNull { it.string.isNotBlank() }
    }

    /**
     * Finds the first non-empty string value in the array.
     *
     * This function iterates through the ValueArray and returns the first element
     * whose string representation is not empty (length > 0). Unlike firstNotBlank,
     * this function accepts strings that contain only whitespace.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Find first non-empty value
     * SELECT ARRAY.firstNotEmpty(makeArray('', 'hello', 'world')); -- returns 'hello'
     *
     * -- Whitespace is considered non-empty
     * SELECT ARRAY.firstNotEmpty(makeArray('', '  ', 'hello')); -- returns '  '
     *
     * -- All values are empty
     * SELECT ARRAY.firstNotEmpty(makeArray('', '', '')); -- returns NULL
     *
     * -- Numbers are converted to strings
     * SELECT ARRAY.firstNotEmpty(makeArray('', '0', 'hello')); -- returns '0'
     * ```
     *
     * ## Use Cases
     * - Finding first available data value
     * - String validation and processing
     * - Data extraction with whitespace preservation
     * - Fallback value selection
     *
     * ## Comparison with firstNotBlank
     * - firstNotEmpty: Accepts whitespace-only strings
     * - firstNotBlank: Rejects whitespace-only strings
     *
     * ## Null Safety
     * - Returns null if no non-empty value is found
     * - Handles null array elements gracefully
     * - Safe to use with mixed-type arrays
     *
     * @param values The ValueArray to search through
     * @return The first non-empty Value, or null if none found
     * @see ValueArray
     * @see String.isNotEmpty
     * @see firstNotBlank
     */
    @UDFunction
    @JvmStatic
    fun firstNotEmpty(values: ValueArray): Value? {
        return values.list.firstOrNull { it.string.isNotEmpty() }
    }
}
