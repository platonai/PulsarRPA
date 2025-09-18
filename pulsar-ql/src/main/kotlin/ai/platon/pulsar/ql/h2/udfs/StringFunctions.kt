/**
 * String manipulation and text processing functions for X-SQL queries in Pulsar QL.
 *
 * This object provides comprehensive string utility functions for text processing,
 * manipulation, and analysis within X-SQL queries. It includes functions for
 * case conversion, string validation, text extraction, and advanced string operations.
 *
 * ## Function Categories
 *
 * ### Case Conversion
 * - [capitalize] - Convert first character to uppercase
 *
 * ### String Validation
 * - [isAlpha] - Check if string contains only alphabetic characters
 * - [isNumeric] - Check if string contains only numeric characters
 * - [isWhitespace] - Check if string contains only whitespace
 * - [isEmpty] - Check if string is empty or null
 *
 * ### String Comparison
 * - [equals] - Compare strings for equality
 * - [equalsIgnoreCase] - Compare strings ignoring case
 * - [startsWith] - Check if string starts with specified prefix
 * - [endsWith] - Check if string ends with specified suffix
 * - [contains] - Check if string contains specified substring
 *
 * ### String Extraction and Manipulation
 * - [length] - Get string length
 * - [substring] - Extract substring from string
 * - [mid] - Extract middle portion of string
 * - [indexOf] - Find position of substring
 * - [lastIndexOf] - Find last position of substring
 * - [split] - Split string into array
 * - [replace] - Replace occurrences of substring
 * - [remove] - Remove occurrences of substring
 * - [difference] - Find difference between two strings
 *
 * ### Encoding and Conversion
 * - [toString] - Convert byte array to string with specified charset
 *
 * ## Usage Examples
 *
 * ```sql
 * -- String validation
 * SELECT STR.isNumeric('12345'); -- returns true
 * SELECT STR.isAlpha('hello'); -- returns true
 * SELECT STR.isEmpty(''); -- returns true
 *
 * -- String comparison
 * SELECT STR.equals('test', 'TEST'); -- returns false
 * SELECT STR.equalsIgnoreCase('test', 'TEST'); -- returns true
 * SELECT STR.startsWith('hello world', 'hello'); -- returns true
 *
 * -- String manipulation
 * SELECT STR.capitalize('hello'); -- returns 'Hello'
 * SELECT STR.length('hello world'); -- returns 11
 * SELECT STR.substring('hello world', 0, 5); -- returns 'hello'
 * SELECT STR.replace('hello world', 'world', 'universe'); -- returns 'hello universe'
 *
 * -- String extraction
 * SELECT STR.indexOf('hello world', 'world'); -- returns 6
 * SELECT STR.split('a,b,c', ','); -- returns array ['a', 'b', 'c']
 * ```
 *
 * ## X-SQL Integration
 *
 * All string functions are automatically registered as H2 database functions under the
 * "STR" namespace. They can be used directly in X-SQL queries and combined with
 * other functions for text processing workflows.
 *
 * ## Performance Notes
 *
 * - Functions use Apache Commons Lang3 for optimal performance
 * - String operations are optimized for common use cases
 * - Null-safe operations throughout all functions
 * - Efficient memory usage for large text processing
 *
 * ## Thread Safety
 *
 * All functions in this object are thread-safe and can be safely used
 * in concurrent query execution contexts.
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see StringUtils
 * @see UDFGroup
 * @see UDFunction
 * @see <a href="https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html">Apache Commons Lang3 StringUtils</a>
 */
package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import org.apache.commons.lang3.StringUtils
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

/**
 * String manipulation functions for X-SQL queries.
 *
 * Provides comprehensive string utility functions for text processing, validation,
 * comparison, and manipulation within X-SQL queries using Apache Commons Lang3.
 */
@SuppressWarnings("unused")
@UDFGroup(namespace = "STR")
object StringFunctions {

    /**
     * Capitalizes the first character of a string.
     *
     * Converts the first character of the input string to uppercase and leaves the rest unchanged.
     * Returns null if the input string is null.
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT STR.capitalize('hello'); -- returns 'Hello'
     * SELECT STR.capitalize('HELLO'); -- returns 'HELLO' (unchanged)
     * SELECT STR.capitalize('hELLO'); -- returns 'HELLO'
     * SELECT STR.capitalize(''); -- returns ''
     * SELECT STR.capitalize(null); -- returns null
     * ```
     *
     * @param str The input string to capitalize
     * @return The capitalized string, or null if input is null
     * @see StringUtils.capitalize
     */
    @UDFunction @JvmStatic fun capitalize(str: String?): String? = StringUtils.capitalize(str)

    /**
     * Checks if a string contains only alphabetic characters.
     *
     * Returns true if the string contains only Unicode letters (a-z, A-Z and other alphabetic characters).
     * Returns false if the string is null, empty, or contains any non-alphabetic characters.
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT STR.isAlpha('hello'); -- returns true
     * SELECT STR.isAlpha('Hello'); -- returns true
     * SELECT STR.isAlpha('hello123'); -- returns false
     * SELECT STR.isAlpha('hello world'); -- returns false (space is not alphabetic)
     * SELECT STR.isAlpha(''); -- returns false
     * SELECT STR.isAlpha(null); -- returns false
     * ```
     *
     * @param str The string to check
     * @return true if string contains only alphabetic characters, false otherwise
     * @see StringUtils.isAlpha
     */
    @UDFunction @JvmStatic fun isAlpha(str: String?): Boolean = StringUtils.isAlpha(str)

    /**
     * Checks if a string contains only numeric characters.
     *
     * Returns true if the string contains only Unicode digits (0-9).
     * Returns false if the string is null, empty, or contains any non-numeric characters.
     * Note that this only accepts the basic Latin digits 0-9.
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT STR.isNumeric('12345'); -- returns true
     * SELECT STR.isNumeric('000123'); -- returns true
     * SELECT STR.isNumeric('123.45'); -- returns false (decimal point)
     * SELECT STR.isNumeric('-123'); -- returns false (minus sign)
     * SELECT STR.isNumeric('123abc'); -- returns false
     * SELECT STR.isNumeric(''); -- returns false
     * SELECT STR.isNumeric(null); -- returns false
     * ```
     *
     * ## Common Use Cases
     * - Validating numeric IDs or codes
     * - Checking if string can be safely converted to number
     * - Data validation in extraction queries
     * - Filtering numeric data from text
     *
     * ## Limitations
     * - Only accepts basic Latin digits 0-9
     * - Does not accept decimal points, minus signs, or other numeric symbols
     * - For broader numeric validation, consider using regular expressions
     *
     * @param str The string to check
     * @return true if string contains only digits 0-9, false otherwise
     * @see StringUtils.isNumeric
     * @see isAlpha
     * @see CommonFunctions.isNumeric for alternative implementation
     */
    @UDFunction @JvmStatic fun isNumeric(str: String?): Boolean = StringUtils.isNumeric(str)
    /**
     * Finds the difference between two strings starting from the first position where they differ.
     *
     * Compares two strings character by character and returns the remainder of the second string
     * starting from the first position where the strings differ. Returns empty string if the
     * strings are identical.
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT STR.difference('abc', 'abc'); -- returns '' (empty string)
     * SELECT STR.difference('abc', 'abcd'); -- returns 'd'
     * SELECT STR.difference('abc', 'abx'); -- returns 'x'
     * SELECT STR.difference('hello', 'world'); -- returns 'world'
     * SELECT STR.difference('test', ''); -- returns ''
     * SELECT STR.difference(null, 'test'); -- returns null
     * ```
     *
     * ## Use Cases
     * - Finding string variations
     * - Detecting suffix differences
     * - Version comparison
     * - Text diff operations
     *
     * @param str The first string to compare
     * @param arg1 The second string to compare against
     * @return The differing portion of the second string, or null if first string is null
     * @see StringUtils.difference
     */
    @UDFunction @JvmStatic fun difference(str: String?, arg1: String): String? = StringUtils.difference(str, arg1)

    /**
     * Removes all occurrences of a substring from a string.
     *
     * Removes all occurrences of the specified substring from the input string.
     * Returns null if the input string is null. If the substring is not found,
     * the original string is returned unchanged.
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT STR.remove('hello world', 'l'); -- returns 'heo word'
     * SELECT STR.remove('hello world', 'world'); -- returns 'hello '
     * SELECT STR.remove('abcabcabc', 'abc'); -- returns ''
     * SELECT STR.remove('test', 'xyz'); -- returns 'test' (no change)
     * SELECT STR.remove(null, 'test'); -- returns null
     * SELECT STR.remove('test', null); -- returns 'test' (no change)
     * ```
     *
     * ## Use Cases
     * - Cleaning unwanted characters or words
     * - Removing formatting markers
     * - Text sanitization
     * - Data preprocessing
     *
     * @param str The input string to process
     * @param arg1 The substring to remove
     * @return The string with all occurrences removed, or null if input is null
     * @see StringUtils.remove
     * @see replace for substitution operations
     */
    @UDFunction @JvmStatic fun remove(str: String?, arg1: String): String? = StringUtils.remove(str, arg1)
//    @UDFunction @JvmStatic fun remove(str: String?, arg1: Char): String? = StringUtils.remove(str, arg1)

    /**
     * Compares two strings for equality, handling null values safely.
     *
     * Returns true if both strings are identical, false otherwise. Unlike the standard
     * String.equals() method, this function safely handles null values - it returns
     * true only if both strings are null, false if only one is null.
     *
     * ## X-SQL Usage
     * ```sql
     * SELECT STR.equals('test', 'test'); -- returns true
     * SELECT STR.equals('test', 'TEST'); -- returns false (case sensitive)
     * SELECT STR.equals('test', 'testing'); -- returns false
     * SELECT STR.equals(null, null); -- returns true
     * SELECT STR.equals('test', null); -- returns false
     * SELECT STR.equals(null, 'test'); -- returns false
     * ```
     *
     * ## Use Cases
     * - Safe string comparison with null handling
     * - Data validation and verification
     * - Exact string matching
     * - Case-sensitive comparisons
     *
     * ## Comparison with equalsIgnoreCase
     * - equals: Case-sensitive comparison
     * - equalsIgnoreCase: Case-insensitive comparison
     *
     * @param str The first string to compare
     * @param arg1 The second string to compare
     * @return true if strings are equal, false otherwise
     * @see StringUtils.equals
     * @see equalsIgnoreCase for case-insensitive comparison
     */
    @UDFunction @JvmStatic fun equals(str: String?, arg1: String): Boolean = StringUtils.equals(str, arg1)
    @UDFunction @JvmStatic fun length(str: String?): Int = StringUtils.length(str)
    @UDFunction @JvmStatic fun toString(str: ByteArray, arg1: String): String? = StringUtils.toString(str, arg1)
    @UDFunction @JvmStatic fun mid(str: String?, arg1: Int, arg2: Int): String? = StringUtils.mid(str, arg1, arg2)
//    @UDFunction @JvmStatic fun indexOf(str: String?, arg1: Int): Int = StringUtils.indexOf(str, arg1)
//    @UDFunction @JvmStatic fun indexOf(str: String?, arg1: Int, arg2: Int): Int = StringUtils.indexOf(str, arg1, arg2)
    @UDFunction @JvmStatic fun indexOf(str: String?, arg1: String): Int = StringUtils.indexOf(str, arg1)
    @UDFunction @JvmStatic fun indexOf(str: String?, arg1: String, arg2: Int): Int = StringUtils.indexOf(str, arg1, arg2)
    @UDFunction @JvmStatic fun isWhitespace(str: String?): Boolean = StringUtils.isWhitespace(str)
    @UDFunction @JvmStatic fun isEmpty(str: String?): Boolean = StringUtils.isEmpty(str)
    @UDFunction @JvmStatic fun equalsIgnoreCase(str: String?, arg1: String): Boolean = StringUtils.equalsIgnoreCase(str, arg1)
    @UDFunction @JvmStatic fun startsWith(str: String?, arg1: String): Boolean = StringUtils.startsWith(str, arg1)
    @UDFunction @JvmStatic fun endsWith(str: String?, arg1: String): Boolean = StringUtils.endsWith(str, arg1)
//    @UDFunction @JvmStatic fun lastIndexOf(str: String?, arg1: Int): Int = StringUtils.lastIndexOf(str, arg1)
    @UDFunction @JvmStatic fun lastIndexOf(str: String?, arg1: String): Int = StringUtils.lastIndexOf(str, arg1)
//    @UDFunction @JvmStatic fun lastIndexOf(str: String?, arg1: Int, arg2: Int): Int = StringUtils.lastIndexOf(str, arg1, arg2)
    @UDFunction @JvmStatic fun lastIndexOf(str: String?, arg1: String, arg2: Int): Int = StringUtils.lastIndexOf(str, arg1, arg2)
    @UDFunction @JvmStatic fun substring(str: String?, start: Int): String? = StringUtils.substring(str, start)
    @UDFunction @JvmStatic fun substring(str: String?, start: Int, end: Int): String? = StringUtils.substring(str, start, end)
    @UDFunction @JvmStatic fun replace(str: String?, arg1: String, arg2: String): String? = StringUtils.replace(str, arg1, arg2)
    @UDFunction @JvmStatic fun replace(str: String?, arg1: String, arg2: String, arg3: Int): String? = StringUtils.replace(str, arg1, arg2, arg3)
    @UDFunction @JvmStatic fun contains(str: String?, arg1: String): Boolean = StringUtils.contains(str, arg1)
    @UDFunction @JvmStatic fun contains22(str: String?, arg1: Int): Boolean = StringUtils.contains(str, arg1)
    @UDFunction @JvmStatic fun split(str: String?, arg1: String, arg2: Int): Array<String> = StringUtils.split(str, arg1, arg2)
    @UDFunction @JvmStatic fun split(str: String?): Array<String> = StringUtils.split(str)
    @UDFunction @JvmStatic fun split(str: String?, arg1: String): Array<String> = StringUtils.split(str, arg1)
    @UDFunction @JvmStatic fun split22(str: String?, arg1: Char): Array<String> = StringUtils.split(str, arg1)
//    @UDFunction @JvmStatic fun join(str: CharArray, arg1: Char): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: IntArray, arg1: Char): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: ShortArray, arg1: Char): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: ByteArray, arg1: Char): String? = StringUtils.join(str, arg1)
    @UDFunction @JvmStatic fun join(str: Array<String>): String? = StringUtils.join(str)
//    @UDFunction @JvmStatic fun join(str: Array<String>, arg1: Char): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: LongArray, arg1: Char): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: Iterator<String>, arg1: String): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: Iterable<String>, arg1: Char): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: Iterable<String>, arg1: String): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: DoubleArray, arg1: Char, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
//    @UDFunction @JvmStatic fun join(str: FloatArray, arg1: Char, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
//    @UDFunction @JvmStatic fun join(str: CharArray, arg1: Char, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
//    @UDFunction @JvmStatic fun join(str: Array<String>, arg1: String, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
//    @UDFunction @JvmStatic fun join(str: LongArray, arg1: Char, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
//    @UDFunction @JvmStatic fun join(str: Array<String>, arg1: Char, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
//    @UDFunction @JvmStatic fun join(str: DoubleArray, arg1: Char): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: FloatArray, arg1: Char): String? = StringUtils.join(str, arg1)
//    @UDFunction @JvmStatic fun join(str: IntArray, arg1: Char, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
//    @UDFunction @JvmStatic fun join(str: ByteArray, arg1: Char, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
//    @UDFunction @JvmStatic fun join(str: ShortArray, arg1: Char, arg2: Int, arg3: Int): String? = StringUtils.join(str, arg1, arg2, arg3)
    @UDFunction @JvmStatic fun join(str: Array<String>, arg1: String): String? = StringUtils.join(str, arg1)
    @UDFunction @JvmStatic fun trim(str: String?): String? = StringUtils.trim(str)
    @UDFunction @JvmStatic fun strip(str: String?, arg1: String): String? = StringUtils.strip(str, arg1)
    @UDFunction @JvmStatic fun strip(str: String?): String? = StringUtils.strip(str)
    @UDFunction @JvmStatic fun isBlank(str: String?): Boolean = StringUtils.isBlank(str)
    @UDFunction @JvmStatic fun repeat(str: String?, arg1: String, arg2: Int): String? = StringUtils.repeat(str, arg1, arg2)
//    @UDFunction @JvmStatic fun repeat(str: Char, arg1: Int): String? = StringUtils.repeat(str, arg1)
    @UDFunction @JvmStatic fun repeat(str: String?, arg1: Int): String? = StringUtils.repeat(str, arg1)
    @UDFunction @JvmStatic fun reverse(str: String?): String? = StringUtils.reverse(str)
    @UDFunction @JvmStatic fun left(str: String?, arg1: Int): String? = StringUtils.left(str, arg1)
    @UDFunction @JvmStatic fun right(str: String?, arg1: Int): String? = StringUtils.right(str, arg1)
    @UDFunction @JvmStatic fun isAnyEmpty(str: Array<String>): Boolean = StringUtils.isAnyEmpty(*str)
    @UDFunction @JvmStatic fun isNoneEmpty(str: Array<String>): Boolean = StringUtils.isNoneEmpty(*str)
    @UDFunction @JvmStatic fun isNotBlank(str: String?): Boolean = StringUtils.isNotBlank(str)
    @UDFunction @JvmStatic fun isAnyBlank(str: Array<String>): Boolean = StringUtils.isAnyBlank(*str)
    @UDFunction @JvmStatic fun isNoneBlank(str: Array<String>): Boolean = StringUtils.isNoneBlank(*str)
    @UDFunction @JvmStatic fun trimToNull(str: String?): String? = StringUtils.trimToNull(str)
    @UDFunction @JvmStatic fun trimToEmpty(str: String?): String? = StringUtils.trimToEmpty(str)
    @UDFunction @JvmStatic fun stripToNull(str: String?): String? = StringUtils.stripToNull(str)
    @UDFunction @JvmStatic fun stripToEmpty(str: String?): String? = StringUtils.stripToEmpty(str)
    @UDFunction @JvmStatic fun stripStart(str: String?, arg1: String): String? = StringUtils.stripStart(str, arg1)
    @UDFunction @JvmStatic fun stripEnd(str: String?, arg1: String): String? = StringUtils.stripEnd(str, arg1)
    @UDFunction @JvmStatic fun stripAll(str: Array<String>): Array<String> = StringUtils.stripAll(*str)
    @UDFunction @JvmStatic fun stripAll(str: Array<String>, arg1: String): Array<String> = StringUtils.stripAll(str, arg1)
    @UDFunction @JvmStatic fun stripAccents(str: String?): String? = StringUtils.stripAccents(str)
    @UDFunction @JvmStatic fun isNotEmpty(str: String?): Boolean = StringUtils.isNotEmpty(str)
    @UDFunction @JvmStatic fun ordinalIndexOf(str: String?, arg1: String, arg2: Int): Int = StringUtils.ordinalIndexOf(str, arg1, arg2)
    @UDFunction @JvmStatic fun indexOfIgnoreCase(str: String?, arg1: String): Int = StringUtils.indexOfIgnoreCase(str, arg1)
    @UDFunction @JvmStatic fun indexOfIgnoreCase(str: String?, arg1: String, arg2: Int): Int = StringUtils.indexOfIgnoreCase(str, arg1, arg2)
    @UDFunction @JvmStatic fun lastOrdinalIndexOf(str: String?, arg1: String, arg2: Int): Int = StringUtils.lastOrdinalIndexOf(str, arg1, arg2)
    @UDFunction @JvmStatic fun lastIndexOfIgnoreCase(str: String?, arg1: String): Int = StringUtils.lastIndexOfIgnoreCase(str, arg1)
    @UDFunction @JvmStatic fun lastIndexOfIgnoreCase(str: String?, arg1: String, arg2: Int): Int = StringUtils.lastIndexOfIgnoreCase(str, arg1, arg2)
    @UDFunction @JvmStatic fun containsIgnoreCase(str: String?, arg1: String): Boolean = StringUtils.containsIgnoreCase(str, arg1)
    @UDFunction @JvmStatic fun containsWhitespace(str: String?): Boolean = StringUtils.containsWhitespace(str)
    @UDFunction @JvmStatic fun indexOfAny(str: String?, arg1: String): Int = StringUtils.indexOfAny(str, arg1)
    @UDFunction @JvmStatic fun containsAny(str: String?, arg1: String): Boolean = StringUtils.containsAny(str, arg1)
    @UDFunction @JvmStatic fun indexOfAnyBut(str: String?, arg1: String): Int = StringUtils.indexOfAnyBut(str, arg1)
    @UDFunction @JvmStatic fun containsOnly(str: String?, arg1: String): Boolean = StringUtils.containsOnly(str, arg1)
    @UDFunction @JvmStatic fun containsNone(str: String?, arg1: String): Boolean = StringUtils.containsNone(str, arg1)
    @UDFunction @JvmStatic fun substringBefore(str: String?, arg1: String): String? = StringUtils.substringBefore(str, arg1)
    @UDFunction @JvmStatic fun substringAfter(str: String?, arg1: String): String? = StringUtils.substringAfter(str, arg1)
    @UDFunction @JvmStatic fun substringBeforeLast(str: String?, arg1: String): String? = StringUtils.substringBeforeLast(str, arg1)
    @UDFunction @JvmStatic fun substringAfterLast(str: String?, arg1: String): String? = StringUtils.substringAfterLast(str, arg1)
    @UDFunction @JvmStatic fun substringBetween(str: String?, arg1: String): String? = StringUtils.substringBetween(str, arg1)
    @UDFunction @JvmStatic fun substringBetween(str: String?, arg1: String, arg2: String): String? = StringUtils.substringBetween(str, arg1, arg2)
    @UDFunction @JvmStatic fun substringsBetween(str: String?, arg1: String, arg2: String): Array<String> = StringUtils.substringsBetween(str, arg1, arg2)
    @UDFunction @JvmStatic fun splitByWholeSeparator(str: String?, arg1: String, arg2: Int): Array<String> = StringUtils.splitByWholeSeparator(str, arg1, arg2)
    @UDFunction @JvmStatic fun splitByWholeSeparator(str: String?, arg1: String): Array<String> = StringUtils.splitByWholeSeparator(str, arg1)
    @UDFunction @JvmStatic fun splitByWholeSeparatorPreserveAllTokens(str: String?, arg1: String, arg2: Int): Array<String> = StringUtils.splitByWholeSeparatorPreserveAllTokens(str, arg1, arg2)
    @UDFunction @JvmStatic fun splitByWholeSeparatorPreserveAllTokens(str: String?, arg1: String): Array<String> = StringUtils.splitByWholeSeparatorPreserveAllTokens(str, arg1)
    @UDFunction @JvmStatic fun splitPreserveAllTokens(str: String?): Array<String> = StringUtils.splitPreserveAllTokens(str)
    @UDFunction @JvmStatic fun splitPreserveAllTokens(str: String?, arg1: String): Array<String> = StringUtils.splitPreserveAllTokens(str, arg1)
    @UDFunction @JvmStatic fun splitPreserveAllTokens2(str: String?, arg1: Char): Array<String> = StringUtils.splitPreserveAllTokens(str, arg1)
    @UDFunction @JvmStatic fun splitPreserveAllTokens(str: String?, arg1: String, arg2: Int): Array<String> = StringUtils.splitPreserveAllTokens(str, arg1, arg2)
    @UDFunction @JvmStatic fun splitByCharacterType(str: String?): Array<String> = StringUtils.splitByCharacterType(str)
    @UDFunction @JvmStatic fun splitByCharacterTypeCamelCase(str: String?): Array<String> = StringUtils.splitByCharacterTypeCamelCase(str)
    @UDFunction @JvmStatic fun deleteWhitespace(str: String?): String? = StringUtils.deleteWhitespace(str)
    @UDFunction @JvmStatic fun removeStart(str: String?, arg1: String): String? = StringUtils.removeStart(str, arg1)
    @UDFunction @JvmStatic fun removeStartIgnoreCase(str: String?, arg1: String): String? = StringUtils.removeStartIgnoreCase(str, arg1)
    @UDFunction @JvmStatic fun removeEnd(str: String?, arg1: String): String? = StringUtils.removeEnd(str, arg1)
    @UDFunction @JvmStatic fun removeEndIgnoreCase(str: String?, arg1: String): String? = StringUtils.removeEndIgnoreCase(str, arg1)
    @UDFunction @JvmStatic fun replaceOnce(str: String?, arg1: String, arg2: String): String? = StringUtils.replaceOnce(str, arg1, arg2)
    @UDFunction @JvmStatic fun replacePattern(str: String?, arg1: String, arg2: String): String? = StringUtils.replacePattern(str, arg1, arg2)
    @UDFunction @JvmStatic fun removePattern(str: String?, arg1: String): String? = StringUtils.removePattern(str, arg1)
    @UDFunction @JvmStatic fun replaceEach(str: String?, arg1: Array<String>, arg2: Array<String>): String? = StringUtils.replaceEach(str, arg1, arg2)
    @UDFunction @JvmStatic fun replaceEachRepeatedly(str: String?, arg1: Array<String>, arg2: Array<String>): String? = StringUtils.replaceEachRepeatedly(str, arg1, arg2)
    @UDFunction @JvmStatic fun replaceChars2(str: String?, arg1: Char, arg2: Char): String? = StringUtils.replaceChars(str, arg1, arg2)
    @UDFunction @JvmStatic fun replaceChars(str: String?, arg1: String, arg2: String): String? = StringUtils.replaceChars(str, arg1, arg2)
    @UDFunction @JvmStatic fun overlay(str: String?, arg1: String, arg2: Int, arg3: Int): String? = StringUtils.overlay(str, arg1, arg2, arg3)
    @UDFunction @JvmStatic fun chomp(str: String?): String? = StringUtils.chomp(str)
    @UDFunction @JvmStatic fun chomp(str: String?, arg1: String): String? = StringUtils.chomp(str, arg1)
    @UDFunction @JvmStatic fun chop(str: String?): String? = StringUtils.chop(str)
    @UDFunction @JvmStatic fun rightPad2(str: String?, arg1: Int, arg2: Char): String? = StringUtils.rightPad(str, arg1, arg2)
    @UDFunction @JvmStatic fun rightPad(str: String?, arg1: Int): String? = StringUtils.rightPad(str, arg1)
    @UDFunction @JvmStatic fun rightPad(str: String?, arg1: Int, arg2: String): String? = StringUtils.rightPad(str, arg1, arg2)
    @UDFunction @JvmStatic fun leftPad2(str: String?, arg1: Int, arg2: Char): String? = StringUtils.leftPad(str, arg1, arg2)
    @UDFunction @JvmStatic fun leftPad(str: String?, arg1: Int): String? = StringUtils.leftPad(str, arg1)
    @UDFunction @JvmStatic fun leftPad(str: String?, arg1: Int, arg2: String): String? = StringUtils.leftPad(str, arg1, arg2)
    @UDFunction @JvmStatic fun center(str: String?, arg1: Int): String? = StringUtils.center(str, arg1)
    @UDFunction @JvmStatic fun center2(str: String?, arg1: Int, arg2: Char): String? = StringUtils.center(str, arg1, arg2)
    @UDFunction @JvmStatic fun center(str: String?, arg1: Int, arg2: String): String? = StringUtils.center(str, arg1, arg2)
    @UDFunction @JvmStatic fun upperCase(str: String?, arg1: Locale): String? = StringUtils.upperCase(str, arg1)
    @UDFunction @JvmStatic fun upperCase(str: String?): String? = StringUtils.upperCase(str)
    @UDFunction @JvmStatic fun lowerCase(str: String?, arg1: Locale): String? = StringUtils.lowerCase(str, arg1)
    @UDFunction @JvmStatic fun lowerCase(str: String?): String? = StringUtils.lowerCase(str)
    @UDFunction @JvmStatic fun uncapitalize(str: String?): String? = StringUtils.uncapitalize(str)
    @UDFunction @JvmStatic fun swapCase(str: String?): String? = StringUtils.swapCase(str)
    @UDFunction @JvmStatic fun countMatches(str: String?, arg1: String): Int = StringUtils.countMatches(str, arg1)
    @UDFunction @JvmStatic fun isAlphaSpace(str: String?): Boolean = StringUtils.isAlphaSpace(str)
    @UDFunction @JvmStatic fun isAlphanumeric(str: String?): Boolean = StringUtils.isAlphanumeric(str)
    @UDFunction @JvmStatic fun isAlphanumericSpace(str: String?): Boolean = StringUtils.isAlphanumericSpace(str)
    @UDFunction @JvmStatic fun isAsciiPrintable(str: String?): Boolean = StringUtils.isAsciiPrintable(str)
    @UDFunction @JvmStatic fun isNumericSpace(str: String?): Boolean = StringUtils.isNumericSpace(str)
    @UDFunction @JvmStatic fun isAllLowerCase(str: String?): Boolean = StringUtils.isAllLowerCase(str)
    @UDFunction @JvmStatic fun isAllUpperCase(str: String?): Boolean = StringUtils.isAllUpperCase(str)
    @UDFunction @JvmStatic fun defaultString(str: String?): String? = StringUtils.defaultString(str)
    @UDFunction @JvmStatic fun defaultString(str: String?, arg1: String): String? = StringUtils.defaultString(str, arg1)
    @UDFunction @JvmStatic fun defaultIfBlank(str: String?, arg1: String): String? = StringUtils.defaultIfBlank(str, arg1)
    @UDFunction @JvmStatic fun defaultIfEmpty(str: String?, arg1: String): String? = StringUtils.defaultIfEmpty(str, arg1)
    @UDFunction @JvmStatic fun reverseDelimited(str: String?, arg1: Char): String? = StringUtils.reverseDelimited(str, arg1)
    @UDFunction @JvmStatic fun abbreviate(str: String?, arg1: Int, arg2: Int): String? = StringUtils.abbreviate(str, arg1, arg2)
    @UDFunction @JvmStatic fun abbreviate(str: String?, arg1: Int): String? = StringUtils.abbreviate(str, arg1)
    @UDFunction @JvmStatic fun abbreviateMiddle(str: String?, arg1: String, arg2: Int): String? = StringUtils.abbreviateMiddle(str, arg1, arg2)
    @UDFunction @JvmStatic fun indexOfDifference(str: Array<String>): Int = StringUtils.indexOfDifference(*str)
    @UDFunction @JvmStatic fun indexOfDifference(str: String?, arg1: String): Int = StringUtils.indexOfDifference(str, arg1)
    @UDFunction @JvmStatic fun getCommonPrefix(str: Array<String>): String? = StringUtils.getCommonPrefix(*str)
    @UDFunction @JvmStatic fun getLevenshteinDistance(str: String?, arg1: String): Int = StringUtils.getLevenshteinDistance(str, arg1)
    @UDFunction @JvmStatic fun getLevenshteinDistance(str: String?, arg1: String, arg2: Int): Int = StringUtils.getLevenshteinDistance(str, arg1, arg2)
    @UDFunction @JvmStatic fun getJaroWinklerDistance(str: String?, arg1: String): Double = StringUtils.getJaroWinklerDistance(str, arg1)
    @UDFunction @JvmStatic fun startsWithIgnoreCase(str: String?, arg1: String): Boolean = StringUtils.startsWithIgnoreCase(str, arg1)
    @UDFunction @JvmStatic fun startsWithAny(str: String?, arg1: Array<String>): Boolean = StringUtils.startsWithAny(str, *arg1)
    @UDFunction @JvmStatic fun endsWithIgnoreCase(str: String?, arg1: String): Boolean = StringUtils.endsWithIgnoreCase(str, arg1)
    @UDFunction @JvmStatic fun normalizeSpace(str: String?): String? = StringUtils.normalizeSpace(str)
    @UDFunction @JvmStatic fun endsWithAny(str: String?, arg1: Array<String>): Boolean = StringUtils.endsWithAny(str, *arg1)
    @UDFunction @JvmStatic fun appendIfMissing(str: String?, arg1: String, arg2: Array<String>): String? = StringUtils.appendIfMissing(str, arg1, *arg2)
    @UDFunction @JvmStatic fun appendIfMissingIgnoreCase(str: String?, arg1: String, arg2: Array<String>): String? = StringUtils.appendIfMissingIgnoreCase(str, arg1, *arg2)
    @UDFunction @JvmStatic fun prependIfMissing(str: String?, arg1: String, arg2: Array<String>): String? = StringUtils.prependIfMissing(str, arg1, *arg2)
    @UDFunction @JvmStatic fun prependIfMissingIgnoreCase(str: String?, arg1: String, arg2: Array<String>): String? = StringUtils.prependIfMissingIgnoreCase(str, arg1, *arg2)
    @UDFunction @JvmStatic fun toEncodedString(str: ByteArray, arg1: Charset): String? = StringUtils.toEncodedString(str, arg1)

    @UDFunction(description = "Get the first integer in the given string")
    @JvmStatic
    fun firstInteger(str: String?, defaultValue: Int): Int = Strings.getFirstInteger(str, defaultValue)

    @UDFunction(description = "Get the first float in the given string")
    @JvmStatic
    fun firstFloat(str: String?, defaultValue: Float): Float = Strings.getFirstFloatNumber(str, defaultValue)

    @UDFunction(description = "Get the first float number in the given string")
    @JvmStatic
    fun getFirstFloatNumber(str: String?, defaultValue: Float): Float {
        return Strings.getFirstFloatNumber(str, defaultValue)
    }

    @UDFunction(description = "Chinese tokenizer")
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun chineseTokenize(str: String?, sep: String = " "): String {
        if (str.isNullOrBlank()) {
            return ""
        }

        val analyzer = SmartChineseAnalyzer()

        val sb = StringBuilder()

        val tokenStream = analyzer.tokenStream("field", str)
        val term = tokenStream.addAttribute(CharTermAttribute::class.java)
        tokenStream.reset()
        while (tokenStream.incrementToken()) {
            sb.append(term.toString())
            sb.append(sep)
        }
        tokenStream.end()
        tokenStream.close()

        return sb.toString()
    }
}
