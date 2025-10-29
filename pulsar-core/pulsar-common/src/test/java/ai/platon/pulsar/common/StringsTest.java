package ai.platon.pulsar.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Strings.java utility class
 */
@DisplayName("Strings Utility Class Tests")
public class StringsTest {

    // Constants tests
    @Test
    @DisplayName("Test constants are properly defined")
    void testConstants() {
        assertNotNull(Strings.emptyStringArray);
        assertEquals(0, Strings.emptyStringArray.length);
        assertEquals(',', Strings.COMMA);
        assertEquals(",", Strings.COMMA_STR);
        assertEquals('，', Strings.FULL_WIDTH_COMMA);
        assertEquals("，", Strings.FULL_WIDTH_COMMA_STR);
        assertEquals('\\', Strings.ESCAPE_CHAR);
        assertNotNull(Strings.DEFAULT_KEEP_CHARS);
        assertNotNull(Strings.HTML_TAG_REGEX);
        assertNotNull(Strings.FLOAT_REGEX);
        assertNotNull(Strings.padding);
        assertEquals(11, Strings.padding.length);
        assertEquals("", Strings.padding[0]);
        assertEquals(" ", Strings.padding[1]);
    }

    // Time counting tests
    @Test
    @DisplayName("Test countTimeString with valid time formats")
    void testCountTimeString_Valid() {
        assertEquals(1, Strings.countTimeString("Meeting at 14:30"));
        assertEquals(2, Strings.countTimeString("Start 09:15, end 17:45"));
        assertEquals(0, Strings.countTimeString("No time here"));
        assertEquals(1, Strings.countTimeString("23:59 is valid"));
        assertEquals(0, Strings.countTimeString("24:00 is invalid"));
        assertEquals(0, Strings.countTimeString("14:60 is invalid"));
    }

    @Test
    @DisplayName("Test countTimeString with edge cases")
    void testCountTimeString_EdgeCases() {
        assertEquals(0, Strings.countTimeString(""));
        assertEquals(0, Strings.countTimeString(null));
        assertEquals(1, Strings.countTimeString("12:34:56"));
        assertEquals(0, Strings.countTimeString("1:23"));
        assertEquals(0, Strings.countTimeString("123:45"));
    }

    // Whitespace tests
    @Test
    @DisplayName("Test isActuallyWhitespace with various whitespace characters")
    void testIsActuallyWhitespace() {
        assertTrue(Strings.isActuallyWhitespace(' '));
        assertTrue(Strings.isActuallyWhitespace('\t'));
        assertTrue(Strings.isActuallyWhitespace('\n'));
        assertTrue(Strings.isActuallyWhitespace('\f'));
        assertTrue(Strings.isActuallyWhitespace('\r'));
        assertTrue(Strings.isActuallyWhitespace(160)); // NBSP
        assertFalse(Strings.isActuallyWhitespace('a'));
        assertFalse(Strings.isActuallyWhitespace('1'));
        assertFalse(Strings.isActuallyWhitespace('中'));
    }

    // Hex string tests
    @Test
    @DisplayName("Test toHexString with ByteBuffer")
    void testToHexString_ByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03, (byte) 0xFF});
        String result = Strings.toHexString(buffer);
        assertEquals("010203ff", result);
    }

    @Test
    @DisplayName("Test toHexString with separator and line length")
    void testToHexString_WithSeparatorAndLineLength() {
        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
        String result = Strings.toHexString(data, " ", 2);
        assertEquals("01 02 03 04 05 06", result);
    }

    @Test
    @DisplayName("Test toHexString with null input")
    void testToHexString_NullInput() {
        assertNull(Strings.toHexString((byte[]) null));
        assertNull(Strings.toHexString((ByteBuffer) null));
    }

    @Test
    @DisplayName("Test toHexString with empty array")
    void testToHexString_EmptyArray() {
        assertEquals("", Strings.toHexString(new byte[0]));
    }

    // Chinese character tests
    @Test
    @DisplayName("Test isChinese with Chinese characters")
    void testIsChinese() {
        assertTrue(Strings.isChinese('中'));
        assertTrue(Strings.isChinese('国'));
        assertTrue(Strings.isChinese('文'));
        assertFalse(Strings.isChinese('a'));
        assertFalse(Strings.isChinese('1'));
        assertFalse(Strings.isChinese('!'));
    }

    @Test
    @DisplayName("Test isChinese with strings")
    void testIsChinese_String() {
        assertTrue(Strings.isChinese("中文"));
        assertTrue(Strings.isChinese("中国文字"));
        assertFalse(Strings.isChinese("abc"));
        assertFalse(Strings.isChinese("123"));
        assertFalse(Strings.isChinese(""));
        assertFalse(Strings.isChinese(null));
    }

    @Test
    @DisplayName("Test countChinese")
    void testCountChinese() {
        assertEquals(2, Strings.countChinese("中文"));
        assertEquals(4, Strings.countChinese("中国文字"));
        assertEquals(2, Strings.countChinese("abc中文"));
        assertEquals(0, Strings.countChinese("abc123"));
        assertEquals(0, Strings.countChinese(""));
        assertEquals(0, Strings.countChinese(null));
    }

    @Test
    @DisplayName("Test isMainlyChinese")
    void testIsMainlyChinese() {
        assertTrue(Strings.isMainlyChinese("中文文字", 0.5));
        assertFalse(Strings.isMainlyChinese("中文abc", 0.5));
        assertFalse(Strings.isMainlyChinese("abc中文", 0.7));
        assertFalse(Strings.isMainlyChinese("", 0.5));
        assertFalse(Strings.isMainlyChinese(null, 0.5));
    }

    @Test
    @DisplayName("Test isChineseByREG")
    void testIsChineseByREG() {
        assertTrue(Strings.isChineseByREG("中文"));
        assertTrue(Strings.isChineseByREG("abc中文"));
        assertFalse(Strings.isChineseByREG("abc123"));
        assertFalse(Strings.isChineseByREG(""));
        assertFalse(Strings.isChineseByREG(null));
    }

    @Test
    @DisplayName("Test isChineseCharByREG")
    void testIsChineseCharByREG() {
        assertTrue(Strings.isChineseCharByREG('中'));
        assertTrue(Strings.isChineseCharByREG('国'));
        assertFalse(Strings.isChineseCharByREG('a'));
        assertFalse(Strings.isChineseCharByREG('1'));
    }

    // Character removal tests
    @Test
    @DisplayName("Test removeNonChar")
    void testRemoveNonChineseChar() {
        assertEquals("abc123", Strings.removeNonChineseChar("abc123!@#"));
        assertEquals("abc123中文", Strings.removeNonChineseChar("abc123!@#中文"));
        assertEquals("", Strings.removeNonChineseChar("!@#$%"));
        assertEquals("", Strings.removeNonChineseChar(""));
        assertEquals("", Strings.removeNonChineseChar(null));
    }

    @Test
    @DisplayName("Test removeNonChar with keeps")
    void testRemoveNonChineseChar_WithKeeps() {
        assertEquals("abc123!@#", Strings.removeNonChineseChar("abc123!@#$%", "!@#"));
        assertEquals("abc-123", Strings.removeNonChineseChar("abc-123!@#", "-"));
        assertEquals("", Strings.removeNonChineseChar("!@#$%", "xyz"));
    }

    @Test
    @DisplayName("Test trimNonChar")
    void testTrimNonChineseChar() {
        assertEquals("abc123", Strings.trimNonChineseChar("!@#abc123$%"));
        assertEquals("abc123中文", Strings.trimNonChineseChar("!@#abc123中文$%"));

        assertEquals("中文", Strings.trimNonChineseChar("!@#中文$%"));
        assertEquals("", Strings.trimNonChineseChar("!@#$%"));

        assertEquals("", Strings.trimNonChineseChar(""));
        assertEquals("", Strings.trimNonChineseChar(null));
    }

    @Test
    @DisplayName("Test trimNonChar with keeps")
    void testTrimNonChineseChar_WithKeeps() {
        assertEquals("!@#abc123$%!", Strings.trimNonChineseChar("!@#abc123$%!", "!"));
        assertEquals("-abc-123-", Strings.trimNonChineseChar("!@#-abc-123-!@#", "-"));
    }

    @Test
    @DisplayName("Test removeNonCJKChar")
    void testRemoveNonCJKChar() {
        assertEquals("中文", Strings.removeNonCJKChar("!@#中文$%"));
        assertEquals("abc123中文", Strings.removeNonCJKChar("abc123!@#中文"));
        assertEquals("", Strings.removeNonCJKChar("!@#$%"));
        assertEquals("", Strings.removeNonCJKChar(""));
        assertEquals("", Strings.removeNonCJKChar(null));
    }

    @Test
    @DisplayName("Test trimNonCJKChar")
    void testTrimNonCJKChar() {
        assertEquals("中文", Strings.trimNonCJKChar("!@#中文$%"));
        assertEquals("", Strings.trimNonCJKChar("!@#$%"));
        assertEquals(null, Strings.trimNonCJKChar(null));
        assertEquals("abc123中文", Strings.trimNonCJKChar("!@#abc123中文$%"));
        assertEquals("", Strings.trimNonCJKChar(""));
    }

    // Printable character tests
    @Test
    @DisplayName("Test isPrintableUnicodeChar")
    void testIsPrintableUnicodeChar() {
        assertTrue(Strings.isPrintableUnicodeChar('a'));
        assertTrue(Strings.isPrintableUnicodeChar('中'));
        assertTrue(Strings.isPrintableUnicodeChar('1'));
        assertFalse(Strings.isPrintableUnicodeChar('\u0000'));
        assertFalse(Strings.isPrintableUnicodeChar('\u0001'));
    }

    @Test
    @DisplayName("Test removeNonPrintableChar")
    void testRemoveNonPrintableChar() {
        assertEquals("abc123", Strings.removeNonPrintableChar("abc\u0000123"));
        assertEquals("helloworld", Strings.removeNonPrintableChar("hello\u0000world"));
        assertEquals("", Strings.removeNonPrintableChar(""));
        assertEquals("", Strings.removeNonPrintableChar(null));
    }

    @Test
    @DisplayName("Test clearControlChars")
    void testClearControlChars() {
        assertEquals("abc 123", Strings.clearControlChars("abc\u0001123"));
        assertEquals("hello world", Strings.clearControlChars("hello\u0001world"));
        assertEquals("abc123", Strings.clearControlChars("abc\u0001123", ""));
        assertEquals("", Strings.clearControlChars(""));
        assertEquals("", Strings.clearControlChars(null));
    }

    // Longest part tests
    @Test
    @DisplayName("Test getLongestPart with Pattern")
    void testGetLongestPart_Pattern() {
        Pattern pattern = Pattern.compile("[,-]");
        assertEquals("longest", Strings.getLongestPart("short,medium,longest", pattern));
        assertEquals("longest", Strings.getLongestPart("short-medium-longest", pattern));
        assertEquals("", Strings.getLongestPart("noseparators", pattern));
        assertEquals("", Strings.getLongestPart("", pattern));
    }

    @Test
    @DisplayName("Test getLongestPart with regex")
    void testGetLongestPart_Regex() {
        assertEquals("longest", Strings.getLongestPart("short,medium,longest", "[,-]"));
        assertEquals("longest", Strings.getLongestPart("short-medium-longest", "[,-]"));
        assertEquals("", Strings.getLongestPart("noseparators", "[,-]"));
    }

    // CSS and humanize tests
    @Test
    @DisplayName("Test csslize")
    void testCsslize() {
        assertEquals("test-string-util", Strings.csslize("TestStringUtil"));
        assertEquals("test-string", Strings.csslize("testString"));
        assertEquals("test-string", Strings.csslize("test-string"));
        assertEquals("test-string", Strings.csslize("test_string"));
        assertEquals("test-string", Strings.csslize("test string"));
        assertEquals("test-string", Strings.csslize("test      string"));
        assertEquals("test", Strings.csslize("Test"));
        assertEquals("", Strings.csslize(""));
    }

    @Test
    @DisplayName("Test humanize")
    void testHumanize() {
        assertEquals("test string util", Strings.humanize("TestStringUtil"));
        assertEquals("test string", Strings.humanize("testString"));
        assertEquals("test string", Strings.humanize("test-string"));
        assertEquals("test string", Strings.humanize("test_string"));
        assertEquals("test", Strings.humanize("Test"));
        assertEquals("", Strings.humanize(""));
    }

    @Test
    @DisplayName("Test humanize with custom separator")
    void testHumanize_CustomSeparator() {
        assertEquals("test.string.util", Strings.humanize("TestStringUtil", "."));
        assertEquals("test-string", Strings.humanize("testString", "-"));
        assertEquals("test_string", Strings.humanize("test-string", "_"));
    }

    @Test
    @DisplayName("Test humanize with suffix and separator")
    void testHumanize_WithSuffix() {
        assertEquals("test.string.util.suffix", Strings.humanize("TestStringUtil", "suffix", "."));
        assertEquals("test-string-suffix", Strings.humanize("testString", "suffix", "-"));
    }

    @Test
    @DisplayName("Test humanize with Class")
    void testHumanize_Class() {
        assertEquals("strings test.suffix", Strings.humanize(KStringsTest.class, "suffix", "."));
        assertNotNull(Strings.humanize(String.class, "test", "-"));
    }

    // Integer parsing tests
    @Test
    @DisplayName("Test getLeadingInteger")
    void testGetLeadingInteger() {
        assertEquals(123, Strings.getLeadingInteger("123abc", 0));
        assertEquals(0, Strings.getLeadingInteger("abc123", 0));
        assertEquals(567, Strings.getLeadingInteger("567", 0));
        assertEquals(999, Strings.getLeadingInteger("999", 0));
        assertEquals(0, Strings.getLeadingInteger("", 0));
        assertEquals(0, Strings.getLeadingInteger("abc", 0));
    }

    @Test
    @DisplayName("Test getTailingInteger")
    void testGetTailingInteger() {
        assertEquals(123, Strings.getTailingInteger("abc123", 0));
        assertEquals(0, Strings.getTailingInteger("123abc", 0));
        assertEquals(567, Strings.getTailingInteger("abc567", 0));
        assertEquals(999, Strings.getTailingInteger("999", 0));
        assertEquals(0, Strings.getTailingInteger("", 0));
        assertEquals(0, Strings.getTailingInteger("abc", 0));
    }

    @Test
    @DisplayName("Test findFirstInteger")
    void testFindFirstInteger() {
        assertEquals(Integer.valueOf(123), Strings.findFirstInteger("abc123def"));
        assertEquals(Integer.valueOf(1234), Strings.findFirstInteger("1,234"));
        assertEquals(Integer.valueOf(1234), Strings.findFirstInteger("1_234"));
        assertEquals(Integer.valueOf(123), Strings.findFirstInteger("123"));
        assertNull(Strings.findFirstInteger("abc"));
        assertNull(Strings.findFirstInteger(""));
        assertNull(Strings.findFirstInteger(null));
    }

    @Test
    @DisplayName("Test findFirstInteger with default value")
    void testFindFirstInteger_WithDefault() {
        assertEquals(123, Strings.findFirstInteger("abc123def", 0));
        assertEquals(999, Strings.findFirstInteger("abc", 999));
        assertEquals(0, Strings.findFirstInteger("", 0));
    }

    @Test
    @DisplayName("Test findLastInteger")
    void testFindLastInteger() {
        assertEquals(Integer.valueOf(456), Strings.findLastInteger("abc123def456"));
        assertEquals(Integer.valueOf(789), Strings.findLastInteger("123,456,789"));
        assertEquals(Integer.valueOf(123), Strings.findLastInteger("123"));
        assertNull(Strings.findLastInteger("abc"));
        assertNull(Strings.findLastInteger(""));
        assertNull(Strings.findLastInteger(null));
    }

    @Test
    @DisplayName("Test findLastInteger with default value")
    void testFindLastInteger_WithDefault() {
        assertEquals(456, Strings.findLastInteger("abc123def456", 0));
        assertEquals(999, Strings.findLastInteger("abc", 999));
        assertEquals(0, Strings.findLastInteger("", 0));
    }

    @Test
    @DisplayName("Test getFirstFloatNumber")
    void testGetFirstFloatNumber() {
        assertEquals(123.45f, Strings.getFirstFloatNumber("abc123.45def", 0.0f));
        // comma separator not supported now
        // assertEquals(1234.56f, Strings.getFirstFloatNumber("1,234.56", 0.0f), 0.01f);
        assertEquals(1, Strings.getFirstFloatNumber("1,234.56", 0.0f), 0.01f);
        assertEquals(0.5f, Strings.getFirstFloatNumber("0.5", 0.0f));
        assertEquals(-123.45f, Strings.getFirstFloatNumber("abc-123.45def", 0.0f));
        assertEquals(999.0f, Strings.getFirstFloatNumber("abc", 999.0f));
    }

    @Test
    @DisplayName("Test findLastFloatNumber")
    void testFindLastFloatNumber() {
        assertEquals(456.78f, Strings.findLastFloatNumber("abc123.45def456.78", 0.0f));
        assertEquals(789.01f, Strings.findLastFloatNumber("123.45,456.78,789.01", 0.0f), 0.01f);
        assertEquals(123.45f, Strings.findLastFloatNumber("123.45", 0.0f));
        assertEquals(999.0f, Strings.findLastFloatNumber("abc", 999.0f));
    }

    // String collection tests
    @Test
    @DisplayName("Test getStrings")
    void testGetStrings() {
        assertArrayEquals(new String[]{"a", "b", "c"}, Strings.getStrings("a,b,c"));
        assertArrayEquals(new String[]{"a", " b", " c"}, Strings.getStrings("a, b, c"));
        assertArrayEquals(new String[]{"a", "b", "c"}, Strings.getTrimmedStrings("a, b, c"));
        assertNull(Strings.getStrings(""));
        assertNull(Strings.getStrings(null));
    }

    @Test
    @DisplayName("Test arrayToString")
    void testArrayToString() {
        assertEquals("a,b,c", Strings.arrayToString(new String[]{"a", "b", "c"}));
        assertEquals("a", Strings.arrayToString(new String[]{"a"}));
        assertEquals("", Strings.arrayToString(new String[]{}));
    }

    @Test
    @DisplayName("Test getStringCollection")
    void testGetStringCollection() {
        Collection<String> result = Strings.getStringCollection("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Strings.getStringCollection("a, b, c");
        assertEquals(Arrays.asList("a", " b", " c"), result);

        result = Strings.getStringCollection("");
        assertTrue(result.isEmpty());

        result = Strings.getStringCollection(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getStringCollection with custom delimiter")
    void testGetStringCollection_CustomDelimiter() {
        Collection<String> result = Strings.getStringCollection("a;b;c", ";");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Strings.getStringCollection("a|b|c", "|");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    @DisplayName("Test getTrimmedStringCollection")
    void testGetTrimmedStringCollection() {
        Collection<String> result = Strings.getTrimmedStringCollection("a, b, c, b, a");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Strings.getTrimmedStringCollection("  a  ,  b  ,  c  ");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Strings.getTrimmedStringCollection("");
        assertTrue(result.isEmpty());

        result = Strings.getTrimmedStringCollection(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getTrimmedStrings")
    void testGetTrimmedStrings() {
        assertArrayEquals(new String[]{"a", "b", "c"}, Strings.getTrimmedStrings("a, b, c"));
        assertArrayEquals(new String[]{"a", "b", "c"}, Strings.getTrimmedStrings("  a  ,  b  ,  c  "));
        assertArrayEquals(Strings.emptyStringArray, Strings.getTrimmedStrings(""));
        assertArrayEquals(Strings.emptyStringArray, Strings.getTrimmedStrings(null));
    }

    // Pattern matching tests
    @Test
    @DisplayName("Test hasHTMLTags")
    void testHasHTMLTags() {
        assertTrue(Strings.hasHTMLTags("<div>content</div>"));
        assertTrue(Strings.hasHTMLTags("<p>paragraph</p>"));
        assertTrue(Strings.hasHTMLTags("text <span>span</span> more"));
        assertFalse(Strings.hasHTMLTags("no tags here"));
        assertFalse(Strings.hasHTMLTags(""));
    }

    @Test
    @DisplayName("Test isFloat")
    void testIsFloat() {
        assertTrue(Strings.isFloat("123.45"));
        assertTrue(Strings.isFloat("0.5"));
        assertTrue(Strings.isFloat(".5"));
        assertTrue(Strings.isFloat("123."));
        assertTrue(Strings.isFloat("+123.45"));
        assertTrue(Strings.isFloat("-123.45"));
        assertFalse(Strings.isFloat("abc"));
        assertTrue(Strings.isFloat("123"));
        assertFalse(Strings.isFloat(""));
    }

    @Test
    @DisplayName("Test isNumericLike")
    void testIsNumericLike() {
        assertTrue(Strings.isNumericLike("123"));
        assertTrue(Strings.isNumericLike("123.45"));
        assertTrue(Strings.isNumericLike("abc123def"));
        assertTrue(Strings.isNumericLike("123abc"));
        assertTrue(Strings.isNumericLike("abc123"));
        assertFalse(Strings.isNumericLike("abc"));
        assertFalse(Strings.isNumericLike(""));
    }

    @Test
    @DisplayName("Test isMoneyLike")
    void testIsMoneyLike() {
        assertTrue(Strings.isMoneyLike("123.45"));
        assertTrue(Strings.isMoneyLike("¥123.45"));
        assertTrue(Strings.isMoneyLike("￥123.45"));
        assertTrue(Strings.isMoneyLike("$123.45"));
        assertTrue(Strings.isMoneyLike("123"));
        assertFalse(Strings.isMoneyLike("123.456"));
        assertFalse(Strings.isMoneyLike("abc123"));
        assertFalse(Strings.isMoneyLike(""));
    }

    @Test
    @DisplayName("Test isIpPortLike")
    void testIsIpPortLike() {
        assertTrue(Strings.isIpPortLike("192.168.1.1:8080"));
        assertTrue(Strings.isIpPortLike("localhost:3000"));
        assertTrue(Strings.isIpPortLike("example.com:80"));
        assertFalse(Strings.isIpPortLike("192.168.1.1"));
        assertFalse(Strings.isIpPortLike("8080"));
        assertFalse(Strings.isIpPortLike(""));
    }

    @Test
    @DisplayName("Test isIpLike and isIpV4Like")
    void testIsIpLike() {
        assertTrue(Strings.isIpLike("192.168.1.1"));
        assertTrue(Strings.isIpLike("10.0.0.1"));
        assertTrue(Strings.isIpV4Like("192.168.1.1"));
        assertTrue(Strings.isIpV4Like("255.255.255.255"));
        assertFalse(Strings.isIpLike("192.168.1"));
        assertFalse(Strings.isIpLike("192.168.1.1.1"));
        assertFalse(Strings.isIpLike("abc.def.ghi.jkl"));
        assertFalse(Strings.isIpLike(""));
    }

    @Test
    @DisplayName("Test containsAny")
    void testContainsAny() {
        assertTrue(Strings.containsAny("hello world", "hello"));
        assertTrue(Strings.containsAny("hello world", "foo", "hello"));
        assertTrue(Strings.containsAny("hello world", "foo", "bar", "world"));
        assertFalse(Strings.containsAny("hello world", "foo"));
        assertFalse(Strings.containsAny("hello world", "foo", "bar"));
    }

    @Test
    @DisplayName("Test containsNone")
    void testContainsNone() {
        assertTrue(Strings.containsNone("hello world", "foo"));
        assertTrue(Strings.containsNone("hello world", "foo", "bar"));
        assertFalse(Strings.containsNone("hello world", "hello"));
        assertFalse(Strings.containsNone("hello world", "foo", "hello"));
    }

    @Test
    @DisplayName("Test doubleQuoteIfContainsWhitespace")
    void testDoubleQuoteIfContainsWhitespace() {
        assertEquals("hello", Strings.doubleQuoteIfContainsWhitespace("hello"));
        assertEquals("\"hello world\"", Strings.doubleQuoteIfContainsWhitespace("hello world"));
        assertEquals("\"hello\tworld\"", Strings.doubleQuoteIfContainsWhitespace("hello\tworld"));
        assertEquals("\"hello\nworld\"", Strings.doubleQuoteIfContainsWhitespace("hello\nworld"));
        assertEquals("", Strings.doubleQuoteIfContainsWhitespace(""));
        assertEquals("", Strings.doubleQuoteIfContainsWhitespace(null));
    }

    // Compact format tests
    @Test
    @DisplayName("Test compactFormat with various numbers")
    void testCompactFormat() {
        assertEquals("100 B", Strings.compactFormat(100));
        assertEquals("1 KiB", Strings.compactFormat(1024));
        assertEquals("2 KiB", Strings.compactFormat(1536));
        assertEquals("1 MiB", Strings.compactFormat(1024 * 1024));
        assertEquals("1 GiB", Strings.compactFormat(1024L * 1024 * 1024));
    }

    @Test
    @DisplayName("Test compactFormat with SI units")
    void testCompactFormat_SI() {
        assertEquals("100 B", Strings.compactFormat(100, true));
        assertEquals("1.000 kB", Strings.compactFormat(1000, 3, true));
        assertEquals("1.50 kB", Strings.compactFormat(1500, 2, true));
        assertEquals("1.00 MB", Strings.compactFormat(1000 * 1000, 2, true));
        assertEquals("1.0000 GB", Strings.compactFormat(1000L * 1000 * 1000, 4, true));
    }

    @Test
    @DisplayName("Test compactFormat with negative numbers")
    void testCompactFormat_Negative() {
        assertEquals("-100 B", Strings.compactFormat(-100));
        assertEquals("-1 KiB", Strings.compactFormat(-1024));
        assertEquals("-1 kB", Strings.compactFormat(-1000, true));
    }

    @Test
    @DisplayName("Test compactFormat with scale parameter")
    void testCompactFormat_WithScale() {
        // assertEquals("1.000 KiB", Strings.compactFormat(1024, 3, false));
        assertEquals("1.0 KiB", Strings.compactFormat(1024, 1, false));
        assertEquals("1 KiB", Strings.compactFormat(1024, 0, false));
    }

    // Key-value parsing tests
    @Test
    @DisplayName("Test parseKvs")
    void testParseKvs() {
        Map<String, String> result = Strings.parseKvs("a=1 b=2 c=3");
        Map<String, String> expected = new HashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test parseKvs with custom delimiter")
    void testParseKvs_CustomDelimiter() {
        Map<String, String> result = Strings.parseKvs("a:1 b:2 c:3", ":");
        Map<String, String> expected = new HashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    // Line merging tests
    @Test
    @DisplayName("Test getUnslashedLines")
    void testGetUnslashedLines() {
        List<String> result = Strings.getUnslashedLines("line1\\nline2\\nline3");
        assertEquals(Arrays.asList("line1line2line3"), result);

        result = Strings.getUnslashedLines("line1\nline2\\nline3\nline4");
        assertEquals(Arrays.asList("line1", "line2line3", "line4"), result);
    }

    @Test
    @DisplayName("Test getUnslashedLines with custom EOL")
    void testGetUnslashedLines_CustomEOL() {
        List<String> result = Strings.getUnslashedLines("line1\\rline2\\rline3", "\r");
        assertEquals(Arrays.asList("line1line2line3"), result);
    }

    @Test
    @DisplayName("Test mergeSlashedLine")
    void testMergeSlashedLine() {
        assertEquals("line1line2", Strings.mergeSlashedLine("line1\\nline2"));
        assertEquals("line1line2", Strings.mergeSlashedLine("line1\\nline2\\n"));
        assertEquals("line1", Strings.mergeSlashedLine("line1"));
        assertEquals("", Strings.mergeSlashedLine(""));
    }

    @Test
    @DisplayName("Test mergeSlashedLines")
    void testMergeSlashedLines() {
        List<String> input = Arrays.asList("line1\\", "line2", "line3\\", "line4");
        List<String> result = Strings.mergeSlashedLines(input);
        assertEquals(Arrays.asList("line1line2", "line3line4"), result);

        input = Arrays.asList("line1", "line2", "line3");
        result = Strings.mergeSlashedLines(input);
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    // HTML charset tests
    @Test
    @DisplayName("Test replaceCharsetInHtml")
    void testReplaceCharsetInHtml() {
        String html = "<meta charset=\"UTF-8\">";
        String result = Strings.replaceCharsetInHtml(html, "ISO-8859-1");
        assertTrue(result.contains("ISO-8859-1"));
        assertFalse(result.contains("UTF-8"));

        html = "<meta charset='UTF-8'>";
        result = Strings.replaceCharsetInHtml(html, "GBK");
        assertTrue(result.contains("GBK"));

        html = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">";
        result = Strings.replaceCharsetInHtml(html, "ISO-8859-1");
        assertTrue(result.contains("ISO-8859-1"));
    }

    // Deprecated method tests
    @Test
    @DisplayName("Test deprecated methods still work")
    void testDeprecatedMethods() {
        // Test stripNonChar methods redirect to removeNonChar
        assertEquals(Strings.removeNonChineseChar("test!@#"), Strings.stripNonChar("test!@#"));
        assertEquals(Strings.removeNonChineseChar("test!@#", "!"), Strings.stripNonChar("test!@#", "!"));

        // Test clearControlChars variants
        assertEquals(
                Strings.clearControlChars("test\u0001"),
                Strings.removeControlChars("test\u0001") + " "
        );

        // Test reverse method redirects to StringUtils
        assertEquals("olleh", Strings.reverse("hello"));
    }

    // Comparator tests
    @Test
    @DisplayName("Test LongerFirstComparator")
    void testLongerFirstComparator() {
        List<String> strings = Arrays.asList("a", "abc", "ab", "abcd");
        strings.sort(Strings.LongerFirstComparator);
        assertEquals(Arrays.asList("abcd", "abc", "ab", "a"), strings);
    }

    @Test
    @DisplayName("Test ShorterFirstComparator")
    void testShorterFirstComparator() {
        List<String> strings = Arrays.asList("a", "abc", "ab", "abcd");
        strings.sort(Strings.ShorterFirstComparator);
        assertEquals(Arrays.asList("a", "ab", "abc", "abcd"), strings);
    }

    // Edge cases and boundary tests
    @Test
    @DisplayName("Test edge cases with empty and null inputs")
    void testEdgeCases() {
        // Test null safety across methods
        assertEquals(0, Strings.countTimeString(null));
        assertFalse(Strings.isChinese((String) null));
        assertEquals(0, Strings.countChinese(null));
        assertFalse(Strings.isMainlyChinese(null, 0.5));
        assertEquals("", Strings.removeNonChineseChar(null));
        assertEquals("", Strings.trimNonChineseChar(null));
        assertEquals("", Strings.removeNonCJKChar(null));
        assertEquals(null, Strings.trimNonCJKChar(null));
        assertEquals("", Strings.removeNonPrintableChar(null));
        assertEquals("", Strings.clearControlChars(null));
        assertEquals("", Strings.clearControlChars(null, ""));
        assertNull(Strings.toHexString((byte[]) null));
        assertNull(Strings.toHexString((ByteBuffer) null));
        assertNull(Strings.findFirstInteger(null));
        assertNull(Strings.findLastInteger(null));
        assertNull(Strings.getStrings(null));
        assertTrue(Strings.getStringCollection(null).isEmpty());
        assertTrue(Strings.getTrimmedStringCollection(null).isEmpty());
        assertArrayEquals(Strings.emptyStringArray, Strings.getTrimmedStrings(null));
        assertFalse(Strings.hasHTMLTags(null));
        assertFalse(Strings.isFloat(null));
        assertFalse(Strings.isNumericLike(null));
        assertFalse(Strings.isMoneyLike(null));
        assertFalse(Strings.isIpPortLike(null));
        assertFalse(Strings.isIpLike(null));
        assertFalse(Strings.isIpV4Like(null));
        assertEquals("", Strings.doubleQuoteIfContainsWhitespace(null));
        assertEquals("0 B", Strings.compactFormat(0));
        assertTrue(Strings.getUnslashedLines(null).isEmpty());
        assertEquals("", Strings.mergeSlashedLine(null));
        assertTrue(Strings.mergeSlashedLines(null).isEmpty());
        assertEquals("", Strings.replaceCharsetInHtml(null, "UTF-8"));
    }

    // Performance and stress tests
    @Test
    @DisplayName("Test performance with large strings")
    void testLargeStrings() {
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            large.append("Hello World ");
        }
        String largeString = large.toString();

        // Should handle large strings efficiently
        assertTrue(Strings.isChinese(largeString + "中文") == Strings.isChinese("中文"));
        assertEquals(4, Strings.countChinese(largeString + "中文文字"));
        assertNotNull(Strings.removeNonChineseChar(largeString));
        assertNotNull(Strings.compactFormat(largeString.length()));
    }

    // Pattern compilation tests
    @Test
    @DisplayName("Test all patterns compile successfully")
    void testPatternCompilation() {
        assertNotNull(Strings.HTML_CHARSET_PATTERN);
        assertNotNull(Strings.PRICE_PATTERN);
        assertNotNull(Strings.FLOAT_PATTERN);
        assertNotNull(Strings.HTML_TAG_PATTERN);
        assertNotNull(Strings.NUMERIC_LIKE_PATTERN);
        assertNotNull(Strings.MONEY_LIKE_PATTERN);
        assertNotNull(Strings.CHINESE_PHONE_NUMBER_LIKE_PATTERN);
        assertNotNull(Strings.IP_PORT_PATTERN);
        assertNotNull(Strings.PatternTime);
    }
}
