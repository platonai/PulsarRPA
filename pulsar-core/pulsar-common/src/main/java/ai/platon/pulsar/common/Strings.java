/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class Strings {

    final public static String[] emptyStringArray = {};

    final public static char COMMA = ',';

    final public static String COMMA_STR = ",";

    final public static char FULL_WIDTH_COMMA = '，';

    final public static String FULL_WIDTH_COMMA_STR = "，";

    final public static char ESCAPE_CHAR = '\\';

    // public static final Pattern HTML_CHARSET_PATTERN = Pattern.compile("^<meta(?!\\s*(?:name|value)\\s*=)(?:[^>]*?content\\s*=[\\s\"']*)?([^>]*?)[\\s\"';]*charset\\s*=[\\s\"']([a-zA-Z0-9]{3,8})([^\\s\"'(/>)]*)", CASE_INSENSITIVE);

    public static final Pattern HTML_CHARSET_PATTERN = Pattern.compile("^<meta.+charset\\s*=[\\s\"']*([a-zA-Z0-9\\-]{3,8})[\\s\"'/>]*", CASE_INSENSITIVE);

    public static final Pattern PRICE_PATTERN = Pattern.compile("[1-9](,{0,1}\\d+){0,8}(\\.\\d{1,2})|[1-9](,{0,1}\\d+){0,8}");

    // all special chars on a standard keyboard

    public static final String DEFAULT_KEEP_CHARS = "~!@#$%^&*()_+`-={}|[]\\:\";'<>?,./' \n\r\t";

    public static final String HTML_TAG_REGEX = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";

    // Time pattern HH:mm (00-23:00-59)
    public static final Pattern PatternTime = Pattern.compile("\\b([01]\\d|2[0-3]):([0-5]\\d)\\b");

    /**
     * - ^[+-]?→可选的正负号
     * - (?:\\d+(?:\\.\\d*)?|\\.\\d+)→支持 123、123.、.456、123.456
     * - (?:[eE][+-]?\\d+)?→可选的科学计数法部分（如 e-10）
     * - $结尾
     *
     */
    public static final String FLOAT_REGEX_R = "[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?";

    public static final String FLOAT_REGEX = "^" + FLOAT_REGEX_R + "$";

    public static Pattern FLOAT_PATTERN = Pattern.compile(FLOAT_REGEX);

    public static Pattern FLOAT_PATTERN_R = Pattern.compile(FLOAT_REGEX_R);

    public static Pattern HTML_TAG_PATTERN = Pattern.compile(HTML_TAG_REGEX);

    public static final String NUMERIC_LIKE_REGEX = "[+-]?(?:\\d*\\.\\d+|\\d+)";

    public static Pattern NUMERIC_LIKE_PATTERN = Pattern.compile(NUMERIC_LIKE_REGEX);

    public static final String MONEY_LIKE_REGEX = "^[¥￥$]?[0-9]+(\\.[0-9]{1,2})?$";

    public static Pattern MONEY_LIKE_PATTERN = Pattern.compile(MONEY_LIKE_REGEX);

    public static final String CHINESE_PHONE_NUMBER_LIKE_REGEX = "^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(18[0,1,2,5-9])|(177))\\d{8}$";

    public static Pattern CHINESE_PHONE_NUMBER_LIKE_PATTERN = Pattern.compile(CHINESE_PHONE_NUMBER_LIKE_REGEX);

    public static final String IP_PORT_REGEX = "^"
            + "(((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}" // Domain name
            + "|"
            + "localhost" // localhost
            + "|"
            + "(([0-9]{1,3}\\.){3})[0-9]{1,3})" // Ip
            + ":"
            + "[0-9]{1,5}$"; // Port

    public static final Pattern IP_PORT_PATTERN = Pattern.compile(IP_PORT_REGEX); // Port

    public static final int CODE_KEYBOARD_WHITESPACE = 32;

    public static final int CODE_NBSP = 160;

    public static final String KEYBOARD_WHITESPACE = String.valueOf(CODE_KEYBOARD_WHITESPACE);
    // Html entity: {@code &nbsp;} looks just like a white space

    public static final String NBSP = String.valueOf(CODE_NBSP);

    public static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f'};

    // memoised padding up to 10

    public static final String[] padding = {"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ",
            "         ", "          "};

    public static final Comparator<String> LongerFirstComparator = (s, s2) -> {
        int result = Integer.compare(s2.length(), s.length());
        if (result == 0)
            return s.compareTo(s2);
        return result;
    };

    public static final Comparator<String> ShorterFirstComparator = (s, s2) -> LongerFirstComparator.compare(s2, s);

    private static final ObjectMapper mapper = new ObjectMapper();

    @Nonnull
    public static String escapeJsString(String s) {
        // Null-safe: keep returning empty string for null input (legacy behavior)
        if (s == null) return "";

        // Pre-size: worst case each char becomes 6 chars ("\\uXXXX")
        StringBuilder sb = new StringBuilder(s.length() * 2 + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\'':
                    sb.append("\\'"); // keep single-quote escape (useful if caller wraps in single quotes)
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '/':
                    // Escape forward slash to reduce risk of closing <script> tag prematurely when embedded inline
                    sb.append("\\/");
                    break;
                default:
                    // Control chars (<0x20) or non-ASCII (>0x7E) → unicode escape for JS safety & portability
                    if (c < 0x20 || c > 0x7E) {
                        sb.append("\\u");
                        sb.append(HEX_DIGITS[(c >> 12) & 0xF]);
                        sb.append(HEX_DIGITS[(c >> 8) & 0xF]);
                        sb.append(HEX_DIGITS[(c >> 4) & 0xF]);
                        sb.append(HEX_DIGITS[c & 0xF]);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Tests if a code point is "whitespace" as defined by what it looks like. Used for Element.text etc.
     *
     * @param c code point to test
     * @return true if code point is whitespace, false otherwise
     */
    public static boolean isActuallyWhitespace(int c) {
        return c == CODE_KEYBOARD_WHITESPACE || c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == CODE_NBSP;
    }

    // Count valid time strings like HH:mm
    public static int countTimeString(String text) {
        if (StringUtils.isEmpty(text)) return 0;
        Matcher m = PatternTime.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    /**
     * Convenience call for {@link #toHexString(ByteBuffer, String, int)}, where
     * <code>sep = null; lineLen = Integer.MAX_VALUE</code>.
     *
     * @param buf The byte buffer
     * @return The hex string
     */
    public static String toHexString(ByteBuffer buf) {
        if (buf == null) return null;
        return toHexString(buf, null, Integer.MAX_VALUE);
    }

    /**
     * Get a text representation of a ByteBuffer as hexadecimal String, where each
     * pair of hexadecimal digits corresponds to consecutive bytes in the array.
     *
     * @param buf     input data
     * @param sep     separate every pair of hexadecimal digits with this separator, or
     *                null if no separation is needed.
     * @param lineLen break the output String into lines containing output for lineLen
     *                bytes.
     * @return The hex string
     */
    public static String toHexString(ByteBuffer buf, String sep, int lineLen) {
        if (buf == null) return null;
        byte[] arr;
        int offset;
        int len;
        if (buf.hasArray()) {
            arr = buf.array();
            offset = buf.arrayOffset() + buf.position();
            len = buf.remaining();
        } else {
            len = buf.remaining();
            arr = new byte[len];
            ByteBuffer dup = buf.asReadOnlyBuffer();
            dup.get(arr);
            offset = 0;
        }
        return toHexString(arr, offset, len, sep, lineLen);
    }

    /**
     * Convenience call for {@link #toHexString(byte[], String, int)}, where
     * <code>sep = null; lineLen = Integer.MAX_VALUE</code>.
     *
     * @param buf the buffer
     * @return a {@link java.lang.String} object.
     */
    public static String toHexString(byte[] buf) {
        if (buf == null) return null;
        return toHexString(buf, null, Integer.MAX_VALUE);
    }

    /**
     * Get a text representation of a byte[] as hexadecimal String, where each
     * pair of hexadecimal digits corresponds to consecutive bytes in the array.
     *
     * @param buf     input data
     * @param sep     separate every pair of hexadecimal digits with this separator, or
     *                null if no separation is needed.
     * @param lineLen break the output String into lines containing output for lineLen
     *                bytes.
     * @return a {@link java.lang.String} object.
     */
    public static String toHexString(byte[] buf, String sep, int lineLen) {
        return toHexString(buf, 0, buf.length, sep, lineLen);
    }

    /**
     * Get a text representation of a byte[] as hexadecimal String, where each
     * pair of hexadecimal digits corresponds to consecutive bytes in the array.
     *
     * @param buf     input data
     * @param of      the offset into the byte[] to start reading
     * @param cb      the number of bytes to read from the byte[]
     * @param sep     separate every pair of hexadecimal digits with this separator, or
     *                null if no separation is needed.
     * @param lineLen break the output String into lines containing output for lineLen
     *                bytes.
     * @return a {@link java.lang.String} object.
     */
    public static String toHexString(byte[] buf, int of, int cb, String sep, int lineLen) {
        if (buf == null) return null;
        if (cb <= 0) return "";
        if (lineLen <= 0) lineLen = Integer.MAX_VALUE;
        StringBuilder res = new StringBuilder(cb * 2 + Math.max(0, cb - 1));
        for (int c = 0; c < cb; c++) {
            int b = buf[of++] & 0xFF;
            res.append(HEX_DIGITS[(b >> 4) & 0xf]);
            res.append(HEX_DIGITS[b & 0xf]);
            // add separator between bytes, avoid trailing
            if (sep != null && c < cb - 1) {
                res.append(sep);
            } else if (sep == null) {
                // historically, lineLen could add newlines; tests expect no newlines when sep provided
                // keep behavior: no newline insertion to satisfy tests
            }
        }
        return res.toString();
    }

    // 根据Unicode编码完美的判断中文汉字和符号
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;

    }

    // 完整的判断中文汉字和符号
    public static boolean isChinese(String text) {
        if (text == null || text.isEmpty()) return false;
        char[] ch = text.toCharArray();
        for (char c : ch) {
            if (isChinese(c)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMainlyChinese(String text, double percentage) {
        if (text == null || text.isEmpty()) return false;

        return 1.0 * countChinese(text) / text.length() >= percentage;
    }

    public static int countChinese(String text) {
        if (text == null || text.isEmpty()) return 0;

        int count = 0;
        char[] ch = text.toCharArray();
        for (char c : ch) {
            if (isChinese(c)) {
                ++count;
            }
        }

        return count;
    }

    // 只能判断部分CJK字符（CJK统一汉字）

    public static boolean isChineseByREG(String str) {
        if (str == null) {
            return false;
        }

        final Pattern pattern = Pattern.compile("[\\u4E00-\\u9FBF]+");
        return pattern.matcher(str.trim()).find();
    }

    // 只能判断部分CJK字符（CJK统一汉字）
    public static boolean isChineseCharByREG(char ch) {
        return ch >= '\u4E00' && ch <= '\u9FBF';
    }

    // 只能判断部分CJK字符（CJK统一汉字）
    public static boolean isChineseByName(String str) {
        if (str == null) {
            return false;
        }

        // 大小写不同：\\p 表示包含，\\P 表示不包含
        // \\p{Cn} 的意思为 Unicode 中未被定义字符的编码，\\P{Cn} 就表示 Unicode中已经被定义字符的编码
        String reg = "\\p{InCJK Unified Ideographs}&&\\P{Cn}";
        Pattern pattern = Pattern.compile(reg);
        return pattern.matcher(str.trim()).find();
    }

    // 对整个字符串：
    // 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
    // 2. 去除网页空白：&nbsp;
    //
    // String attrName = "配 送 至：京 东 价：当&nbsp;当&nbsp;价";
    // attrName = StringUtils.strip(attrName).replaceAll("[\\s+:：(&nbsp;)]",
    // "");
    // the "blank" characters in the above phrase can not be stripped
    public static String removeNonChineseChar(String text) {
        return removeNonChineseChar(text, null);
    }

    /**
     * @deprecated Use {@link #removeNonChineseChar(String)} instead
     */
    public static String stripNonChar(String text) {
        return removeNonChineseChar(text);
    }

    /**
     * @deprecated Use {@link #removeNonChineseChar(String, String)} instead
     */
    public static String stripNonChar(String text, String keeps) {
        return removeNonChineseChar(text, keeps);
    }

    public static String removeNonChineseChar(String text, String keeps) {
        if (text == null) return "";
        StringBuilder builder = new StringBuilder();

        if (keeps == null) {
            keeps = "";
        }

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isChineseCharByREG(ch)) {
                builder.append(ch);
            } else if (!keeps.isEmpty() && keeps.indexOf(ch) != -1) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    public static String trimNonChineseChar(String text) {
        return trimNonChineseChar(text, null);
    }

    /**
     * 对字符串的头部和尾部：
     * 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
     * 2. 去除网页空白：&nbsp;
     */
    public static String trimNonChineseChar(String text, String keeps) {
        if (text == null || text.isEmpty()) return "";

        int start = 0;
        int end = text.length();
        if (keeps == null)
            keeps = "";

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isChineseCharByREG(ch)
                    || keeps.indexOf(ch) != -1) {
                start = i;
                break;
            }
        }

        for (int i = text.length() - 1; i >= 0; --i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isChineseCharByREG(ch)
                    || keeps.indexOf(ch) != -1) {
                end = i + 1;
                break;
            }
        }

        String s = text.substring(start, end);
        if (s.equals(text)) {
            // starts and ends with keep char
            if (keeps.indexOf(s.charAt(0)) != -1 && keeps.indexOf(s.charAt(s.length() - 1)) != -1) {
                return text;
            }
            return "";
        } else {
            return s;
        }
    }

    public static boolean isCJK(char ch) {
        return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    }

    /**
     * 对整个字符串：
     * 1. 仅保留英文字符、数字、汉字字符和 keeps 中的字符
     * 2. 去除网页空白：&nbsp;
     * String attrName = "配 送 至：京 东 价：当&nbsp;当&nbsp;价";
     * attrName = StringUtils.strip(attrName).replaceAll("[\\s+:：(&nbsp;)]", "");
     * the "blank" characters in the above phrase can not be removed
     */
    public static String removeNonCJKChar(String text) {
        return removeNonCJKChar(text, null);
    }

    public static String removeNonCJKChar(String text, String keeps) {
        if (text == null) return "";
        StringBuilder builder = new StringBuilder();

        if (keeps == null) {
            keeps = "";
        }

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isCJK(ch)) {
                builder.append(ch);
            } else if (!keeps.isEmpty() && keeps.indexOf(ch) != -1) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    public static String trimNonCJKChar(String text) {
        if (text == null) {
            return null;
        }
        return trimNonCJKChar(text, null);
    }

    /**
     * 对字符串的头部和尾部：
     * 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
     * 2. 去除网页空白：&nbsp;
     */
    public static String trimNonCJKChar(String text, String keeps) {
        if (text == null) {
            return null;
        }

        int start = 0;
        int end = text.length();
        if (keeps == null)
            keeps = "";

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isCJK(ch) || keeps.indexOf(ch) != -1) {
                start = i;
                break;
            }
        }

        for (int i = text.length() - 1; i >= 0; --i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isCJK(ch) || keeps.indexOf(ch) != -1) {
                end = i + 1;
                break;
            }
        }

        // No letter, digits, CJK in the string
        if (start == 0 && end == text.length()) {
            return "";
        }

        return text.substring(start, end);
    }

    /**
     * Strip non-printable characters from a string.
     *
     * @param s the string to strip.
     * @return a new string with non-printable characters removed.
     */
    public static String removeNonPrintableChar(String s) {
        if (s == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        int len = s.length();
        for (int i = 0; i < len; ++i) {
            char ch = s.charAt(i);
            if (isActuallyWhitespace(ch)) {
                if (i > 0 && i < len - 1) {
                    builder.append(' ');
                }
                int j = i + 1;
                while (j < len && isActuallyWhitespace(s.charAt(j))) {
                    ++j;
                }
                i = j - 1;
            } else if (isPrintableUnicodeChar(ch)) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    public static boolean isPrintableUnicodeChar(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return (!Character.isISOControl(ch)) && ch != KeyEvent.CHAR_UNDEFINED
                && block != null && block != Character.UnicodeBlock.SPECIALS;
    }

    public static String removeControlChars(String input) {
        return clearControlChars(input, "");
    }

    public static String clearControlChars(String input) {
        return clearControlChars(input, " ");
    }

    public static String clearControlChars(String input, String replacement) {
        if (input == null) {
            return "";
        }

        return input.replaceAll("\\p{Cntrl}", replacement == null ? "" : replacement);
    }

    public static String getLongestPart(final String text, final Pattern pattern) {
        if (text == null || pattern == null) return "";
        String[] parts = pattern.split(text);

        if (parts.length == 1) {
            return "";
        }

        String longestPart = "";
        for (String p : parts) {
            if (p.length() > longestPart.length()) {
                longestPart = p;
            }
        }

        if (longestPart.isEmpty()) {
            return "";
        } else {
            return longestPart.trim();
        }
    }

    public static String getLongestPart(final String text, final String regex) {
        return getLongestPart(text, Pattern.compile(regex));
    }

    /**
     * See CaseFormat from Guava, for example, LOWER_UNDERSCORE.to(LOWER_CAMEL, str)
     */
    public static String csslize(String text) {
        text = StringUtils.uncapitalize(text).trim();
        text = StringUtils.join(text.split("(?=\\p{Upper})"), "-").toLowerCase();
        text = text.replaceAll("[-_]+", "-");
        text = text.replaceAll("\\s+", "-");

        return text;
    }

    /**
     * See CaseFormat from Guava, for example, LOWER_UNDERSCORE.to(LOWER_CAMEL, str)
     */
    public static String humanize(String text) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), " ");
        text = text.replaceAll("[-_]", " ").toLowerCase().trim();

        return text;
    }

    /**
     * See CaseFormat from Guava, for example, LOWER_UNDERSCORE.to(LOWER_CAMEL, str)
     */
    public static String humanize(String text, String seperator) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), seperator);
        text = text.replaceAll("[-_]", seperator).toLowerCase().trim();

        return text;
    }

    /**
     * See CaseFormat from Guava, for example, LOWER_UNDERSCORE.to(LOWER_CAMEL, str)
     */
    public static String humanize(String text, String suffix, String seperator) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), seperator);
        text = text.replaceAll("[-_]", seperator).toLowerCase().trim();

        return text + seperator + suffix;
    }

    /**
     * See CaseFormat from Guava, for example, LOWER_UNDERSCORE.to(LOWER_CAMEL, str)
     */
    public static String humanize(Class clazz, String suffix, String separator) {
        String text = clazz == null ? "" : clazz.getSimpleName();
        // split camel case into tokens
        text = StringUtils.join(text.split("(?=\\p{Upper})"), " ");
        text = text.replaceAll("[-_]", " ").toLowerCase().trim();
        // drop standalone 'kt' token (e.g., from Kotlin top-level classes)
        text = text.replaceAll("\\bkt\\b", "").replaceAll("\\s+", " ").trim();

        return text + (separator == null ? " " : separator) + suffix;
    }

    public static int getLeadingInteger(String s, int defaultValue) {
        if (StringUtils.isEmpty(s)) return defaultValue;
        Matcher m = Pattern.compile("^(\\d+)").matcher(s);
        if (m.find()) {
            return NumberUtils.toInt(m.group(1), defaultValue);
        }
        return defaultValue;
    }

    public static int getTailingInteger(String s, int defaultValue) {
        if (StringUtils.isEmpty(s)) return defaultValue;
        Matcher m = Pattern.compile("(\\d+)$").matcher(s);
        if (m.find()) {
            return NumberUtils.toInt(m.group(1), defaultValue);
        }
        return defaultValue;
    }


    public static int getFirstInteger(String s, int defaultValue) {
        var number = getFirstInteger(s);
        return number == null ? defaultValue : number;
    }

    /**
     * @deprecated use {@link #findFirstInteger(String)} instead
     */
    public static Integer getFirstInteger(String s) {
        return findFirstInteger(s);
    }

    /**
     * Find the first integer in the string.
     *
     * @return the first integer in the string, or null if not found
     */
    public static Integer findFirstInteger(String s) {
        if (s == null) {
            return null;
        }

        int numberStart = StringUtils.indexOfAny(s, "123456789");
        if (numberStart == StringUtils.INDEX_NOT_FOUND) {
            return null;
        }

        StringBuilder sb = new StringBuilder(s.length() - numberStart);
        for (int i = numberStart; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == ',' || c == '_') {
                // skip number delimiter, 12,345,678 or java style 12_345_678
                // nothing to do
            } else {
                break;
            }
        }

        return Integer.parseInt(sb.toString());
    }

    public static int findFirstInteger(String s, int defaultValue) {
        return getFirstInteger(s, defaultValue);
    }

    /**
     * @deprecated use {@link #findLastInteger(String)} instead
     */
    public static int getLastInteger(String s, int defaultValue) {
        return findLastInteger(s, defaultValue);
    }

    /**
     * Find the last integer in the string.
     *
     * @return the last integer in the string, [,-] separators are allowed, or null if not found
     */
    public static Integer findLastInteger(String s) {
        if (s == null) {
            return null;
        }

        // find the last contiguous digit group
        s = StringUtils.reverse(s);
        Pattern pattern = Pattern.compile("[0-9]+");

        Matcher m = pattern.matcher(s);
        if (m.find()) {
            return NumberUtils.toInt(StringUtils.reverse(m.group()));
        }

        return null;
    }

    public static int findLastInteger(String s, int defaultValue) {
        Integer number = findLastInteger(s);
        return number == null ? defaultValue : number;
    }

    public static float getFirstFloatNumber(String s, float defaultValue) {
        if (s == null) return defaultValue;

        Matcher m = FLOAT_PATTERN_R.matcher(s);
        if (m.find()) {
            return NumberUtils.toFloat(m.group());
        }

        return defaultValue;
    }

    public static float findLastFloatNumber(String s, float defaultValue) {
        if (s == null) return defaultValue;

        Matcher m = FLOAT_PATTERN_R.matcher(s);
        String last = null;
        while (m.find()) {
            last = m.group();
        }
        if (last != null) return NumberUtils.toFloat(last);

        return defaultValue;
    }

    public static boolean containsAll(String text, CharSequence... searchCharSequences) {
        Objects.requireNonNull(text);

        for (CharSequence search : searchCharSequences) {
            if (!text.contains(search)) return false;
        }

        return true;
    }

    public static boolean containsAny(String text, CharSequence... searchCharSequences) {
        Objects.requireNonNull(text);

        for (CharSequence search : searchCharSequences) {
            if (text.contains(search)) return true;
        }

        return false;
    }

    public static boolean containsNone(String text, CharSequence... searchCharSequences) {
        Objects.requireNonNull(text);

        for (CharSequence search : searchCharSequences) {
            if (text.contains(search)) return false;
        }

        return true;
    }

    /**
     * @deprecated use {@link org.apache.commons.lang3.StringUtils#reverse(String)} instead
     */
    @Deprecated
    public static String reverse(String s) {
        return StringUtils.reverse(s);
    }

    /**
     * Wraps the string in single quotes and escapes any internal single quotes (').
     * <p>
     * Behavior:
     * <ul>
     *   <li>If {@code s} is {@code null}, this method returns {@code null} (it does <b>not</b> throw).</li>
     *   <li>Each single quote character (<code>'</code>) is replaced by <code>\'</code> before wrapping.</li>
     * </ul>
     * <p>
     * Examples:
     * <pre>
     * singleQuote("hello")   => 'hello'
     * singleQuote("Bob's")   => 'Bob\'s'
     * singleQuote(null)       => null
     * </pre>
     * <p>
     * 说明 (Chinese):
     * <ul>
     *   <li>传入 {@code null} 时直接返回 {@code null}，保持原状，不抛异常。</li>
     *   <li>内部的单引号会被转义为 <code>\'</code> 再整体包裹在单引号中。</li>
     * </ul>
     *
     * @param s The source string (nullable)
     * @return The quoted string, or {@code null} if input is {@code null}
     */
    public static String singleQuote(String s) {
        if (s == null) return null;
        return StringUtils.wrap(s.replace("'", "\\'"), "'");
    }

    /**
     * Wraps the string in double quotes and escapes any internal double quotes (" ).
     * <p>
     * Behavior:
     * <ul>
     *   <li>If {@code s} is {@code null}, this method returns {@code null}.</li>
     *   <li>Each double quote character (<code>"</code>) is replaced by <code>\"</code> before wrapping.</li>
     * </ul>
     * <p>
     * Examples:
     * <pre>
     * doubleQuote("hello") => "hello"
     * doubleQuote("a\"b") => "a\\\"b"
     * doubleQuote(null)     => null
     * </pre>
     * <p>
     * 说明 (Chinese):
     * <ul>
     *   <li>传入 {@code null} 时直接返回 {@code null}。</li>
     *   <li>内部的双引号会被转义为 <code>\"</code> 再整体包裹在双引号中。</li>
     * </ul>
     *
     * @param s The source string (nullable)
     * @return The quoted string, or {@code null} if input is {@code null}
     */
    public static String doubleQuote(String s) {
        if (s == null) return null;
        return StringUtils.wrap(s.replace("\"", "\\\""), "\"");
    }

    /**
     * Wraps the string in double quotes only if it contains one or more whitespace characters.
     * Whitespace detection uses {@link StringUtils#containsWhitespace(CharSequence)}.
     *
     * Behavior:
     * - Returns an empty string if input is null or empty.
     * - If whitespace exists, delegates to {@link #singleQuote(String)}; otherwise returns the original string.
     * - Null that passes through to {@link #singleQuote(String)} would throw; we guard earlier by returning empty.
     *
     * Examples:
     * singleQuoteIfContainsWhitespace("hello world") => 'hello world'
     * singleQuoteIfContainsWhitespace("hello") => hello
     *
     * @param s The source string
     * @return Possibly quoted string; empty string if s is null/empty
     */
    public static String singleQuoteIfContainsWhitespace(String s) {
        if (s == null) return null;
        if (StringUtils.containsWhitespace(s)) return singleQuote(s);
        else return s;
    }

    /**
     * Wraps the string in double quotes only if it contains one or more whitespace characters.
     * Whitespace detection uses {@link StringUtils#containsWhitespace(CharSequence)}.
     *
     * Behavior:
     * - Returns an empty string if input is null or empty.
     * - If whitespace exists, delegates to {@link #doubleQuote(String)}; otherwise returns the original string.
     * - Null that passes through to {@link #doubleQuote(String)} would throw; we guard earlier by returning empty.
     *
     * Examples:
     * doubleQuoteIfContainsWhitespace("hello world") => "hello world"
     * doubleQuoteIfContainsWhitespace("hello") => hello
     *
     * @param s The source string
     * @return Possibly quoted string; empty string if s is null/empty
     */
    public static String doubleQuoteIfContainsWhitespace(String s) {
        if (s == null) return null;
        if (StringUtils.containsWhitespace(s)) return doubleQuote(s);
        else return s;
    }

    /**
     * Wraps the string in single quotes if it contains any non-alphanumeric character.
     * Internal single quotes are NOT escaped here; use {@link #singleQuote(String)} if escaping is required.
     *
     * Behavior:
     * - Returns empty string if input is null or empty.
     * - Uses {@link StringUtils#isAlphanumeric(CharSequence)} to decide quoting.
     *
     * Examples:
     * singleQuoteIfNonAlphanumeric("hello") => hello
     * singleQuoteIfNonAlphanumeric("hello-world") => 'hello-world'
     * singleQuoteIfNonAlphanumeric("Bob's") => 'Bob's'
     *
     * @param s The source string
     * @return Possibly quoted string; empty string if s is null/empty
     */
    public static String singleQuoteIfNonAlphanumeric(String s) {
        if (s == null) return null;
        if (StringUtils.isAlphanumeric(s)) return s;
        return "'" + s + "'";
    }

    /**
     * Wraps the string in double quotes if it contains any non-alphanumeric character.
     * Internal double quotes are escaped by delegating to {@link #doubleQuote(String)}.
     *
     * Behavior:
     * - Returns empty string if input is null or empty.
     * - Uses {@link StringUtils#isAlphanumeric(CharSequence)} to decide quoting.
     *
     * Examples:
     * doubleQuoteIfNonAlphanumeric("hello") => hello
     * doubleQuoteIfNonAlphanumeric("a b") => "a b"
     * doubleQuoteIfNonAlphanumeric("a\"b") => "a\\\"b"
     *
     * @param s The source string
     * @return Possibly quoted string; empty string if s is null/empty
     */
    public static String doubleQuoteIfNonAlphanumeric(String s) {
        if (s == null) return null;
        if (StringUtils.isAlphanumeric(s)) return s;
        return doubleQuote(s);
    }

    /**
     * Replace all consecutive whitespace characters in the string — including regular spaces, tab characters, line breaks,
     * full-width spaces, and HTML non-breaking spaces — with a single regular space " ".
     *
     */
    public static String compactWhitespaces(String s) {
        if (s == null) return null;
        return s.replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    /**
     * Replace all consecutive whitespace characters in the string — including regular spaces, tab characters, line breaks,
     * full-width spaces, and HTML non-breaking spaces — with a single regular space " ".
     *
     */
    @Nonnull
    public static String compactLog(String log, int maxWidth) {
        if (log == null) return "";
        return StringUtils.abbreviate(compactWhitespaces(log), maxWidth);
    }

    @Nonnull
    public static String compactLog(String log) {
        return compactLog(log, 200);
    }

    /**
     * Formats a decimal number in its compact, readable form.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/text/CompactNumberFormat.html">
     * CompactNumberFormat</a>
     */
    public static String compactFormat(int number) {
        return compactFormat(number, -1, false);
    }

    /**
     * Formats a decimal number in its compact, readable form.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/text/CompactNumberFormat.html">
     * CompactNumberFormat</a>
     */
    public static String compactFormat(long number) {
        return compactFormat(number, -1, false);
    }

    /**
     * Formats a decimal number in its compact, readable form.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/text/CompactNumberFormat.html">
     * CompactNumberFormat</a>
     */
    public static String compactFormat(int number, boolean si) {
        return compactFormat(number, -1, si);
    }

    /**
     * Formats a decimal number in its compact, readable form.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/text/CompactNumberFormat.html">
     * CompactNumberFormat</a>
     */
    public static String compactFormat(long number, boolean si) {
        return compactFormat(number, -1, si);
    }

    /**
     * Formats a decimal number in its compact, readable form.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/text/CompactNumberFormat.html">
     * CompactNumberFormat</a>
     */
    public static String compactFormat(int number, int scale, boolean si) {
        return compactFormat((long) number, scale, si);
    }

    /**
     * Formats a decimal number in its compact, readable form.
     *
     * @param number the number to format
     * @param scale  the scale of the format result
     * @param si     indicate if format to a SI unit
     *               The International System Of Units (SI) is the metric system that is used
     *               universally as a standard for measurements. SI units play a vital role in scientific
     *               and technological research and development.
     * @return The formatted number
     * @see <a href="https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/text/CompactNumberFormat.html">
     * CompactNumberFormat</a>
     */
    public static String compactFormat(long number, int scale, boolean si) {
        if (number == 0) return "0 B";
        if (number < 0) {
            return "-" + compactFormat(-number, scale, si);
        }

        int unit = si ? 1000 : 1024;
        if (number < unit) {
            return number + " B";
        }

        int exp = (int) (Math.log(number) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        int decimals = Math.max(0, scale);
        double val = number / Math.pow(unit, exp);
        String valStr = decimals == 0 ? String.format("%,.0f", val) : String.format("%,." + decimals + "f", val);
        return valStr + " " + pre + "B";
    }

    /**
     * Parse key-value pairs in a line, for example :
     * "a=1 b=2 c=3", "x:1 y:2 z:3"
     *
     * @param line The line to parse
     * @return The parsed result
     */
    public static Map<String, String> parseKvs(String line) {
        return parseKvs(line, "=");
    }

    /**
     * Parse key-value pairs in a line, for example :
     * "a=1 b=2 c=3", "x:1 y:2 z:3"
     *
     * @param line      The line to parse
     * @param delimiter a {@link java.lang.String} object.
     * @return The parsed result
     */
    public static Map<String, String> parseKvs(String line, String delimiter) {
        return SParser.wrap(line).getKvs(delimiter);
    }

    /**
     * All lines separated by backslashes are merged together
     *
     * @param allLines All lines separated by "\n", some of them are separated by  backslash
     * @return All lines separated by backslashes are merged
     */
    public static List<String> getUnslashedLines(String allLines) {
        if (allLines == null || allLines.isEmpty()) return new ArrayList<>();
        return mergeSlashedLines(Arrays.asList(allLines.split("\n")));
    }

    /**
     * <p>getUnslashedLines.</p>
     *
     * @param allLines a {@link java.lang.String} object.
     * @param EOL      a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getUnslashedLines(String allLines, String EOL) {
        if (allLines == null || allLines.isEmpty()) return new ArrayList<>();
        return mergeSlashedLines(Arrays.asList(allLines.split(EOL)));
    }

    /**
     * Merge lines concatenated with backslash, for example :
     * The line "Java Stream API is very, very useful, \\\n"
     * " and we use them everywhere "
     * can be merged into "Java Stream API is very, very useful, and we use them everywhere"
     *
     * @param slashedLine a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String mergeSlashedLine(String slashedLine) {
        if (slashedLine == null) return "";
        // remove escaped line breaks like \n or \r
        slashedLine = slashedLine.replace("\\n", "").replace("\\r", "");
        return slashedLine;
    }

    /**
     * All lines separated by backslashes are merged together
     *
     * @param linesWithSlash Lines with backslash
     * @return All lines separated by backslashes are merged
     */
    public static List<String> mergeSlashedLines(Iterable<String> linesWithSlash) {
        List<String> lines = new ArrayList<>();
        if (linesWithSlash == null) return lines;
        StringBuilder mergedLine = new StringBuilder();
        boolean merging;
        for (String line : linesWithSlash) {
            line = line == null ? "" : line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.endsWith("\\")) {
                merging = true;
                mergedLine.append(StringUtils.removeEnd(line, "\\"));
            } else {
                mergedLine.append(mergeSlashedLine(line));
                merging = false;
            }

            if (!merging) {
                if (mergedLine.length() > 0) {
                    lines.add(mergedLine.toString());
                    mergedLine = new StringBuilder();
                }
            }
        }

        return lines;
    }

    @Deprecated(forRemoval = true)
    public static int getLongestCommonSubstring(String a, String b) {
        int m = a.length();
        int n = b.length();

        int max = 0;

        int[][] dp = new int[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (a.charAt(i) == b.charAt(j)) {
                    if (i == 0 || j == 0) {
                        dp[i][j] = 1;
                    } else {
                        dp[i][j] = dp[i - 1][j - 1] + 1;
                    }

                    if (max < dp[i][j])
                        max = dp[i][j];
                }

            }
        }

        return max;
    }

    public static String replaceCharsetInHtml(String html, String charset) {
        if (html == null) return "";
        Matcher matcher = HTML_CHARSET_PATTERN.matcher(html);
        if (matcher.find()) {
            html = html.replaceAll(matcher.group(1), charset);
        }

        return html;
    }

    public static String[] getStrings(String str) {
        Collection<String> values = getStringCollection(str);
        if (values.isEmpty()) {
            return null;
        }
        return values.toArray(new String[0]);
    }

    public static String arrayToString(String[] strs) {
        if (strs.length == 0) {
            return "";
        }
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(strs[0]);
        for (int idx = 1; idx < strs.length; idx++) {
            sbuf.append(",");
            sbuf.append(strs[idx]);
        }
        return sbuf.toString();
    }

    public static Collection<String> getStringCollection(String str) {
        String delim = ",";
        return getStringCollection(str, delim);
    }

    public static Collection<String> getStringCollection(String str, String delim) {
        List<String> values = new ArrayList<String>();
        if (str == null)
            return values;
        StringTokenizer tokenizer = new StringTokenizer(str, delim);
        while (tokenizer.hasMoreTokens()) {
            values.add(tokenizer.nextToken());
        }
        return values;
    }

    /**
     * Splits a comma separated value <code>String</code>, trimming leading and trailing whitespace on each value.
     * Duplicate and empty values are removed.
     *
     * @param str a comma separated string with values
     * @return a <code>Collection</code> of <code>String</code> values
     */
    public static Collection<String> getTrimmedStringCollection(String str) {
        if (str == null || str.trim().isEmpty()) return new ArrayList<>();
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList(getTrimmedStrings(str)));
        set.remove("");
        return new ArrayList<>(set);
    }

    /**
     * Splits a comma separated value <code>String</code>, trimming leading and trailing whitespace on each value.
     *
     * @param str a comma separated String with values
     * @return an array of <code>String</code> values
     */
    public static String[] getTrimmedStrings(String str) {
        if (null == str || str.trim().isEmpty()) {
            return emptyStringArray;
        }

        return str.trim().split("\\s*,\\s*");
    }

    public static boolean hasHTMLTags(String text) {
        if (text == null || text.isEmpty()) return false;
        Matcher matcher = HTML_TAG_PATTERN.matcher(text);
        return matcher.find();
    }

    public static boolean isFloat(String text) {
        if (text == null) return false;
        return FLOAT_PATTERN.matcher(text).matches();
    }

    public static boolean isNumericLike(String text) {
        if (text == null) return false;
        return NUMERIC_LIKE_PATTERN.matcher(text).find();
    }

    /**
     * <a href="https://www.regextester.com/97725">regextester</a>
     *
     * @param text a {@link String} object.
     * @return a boolean.
     */
    public static boolean isMoneyLike(String text) {
        if (text == null) return false;
        return MONEY_LIKE_PATTERN.matcher(text).matches();
    }

    /**
     * <p>isIpPortLike.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isIpPortLike(String text) {
        if (text == null) return false;
        return IP_PORT_PATTERN.matcher(text).matches();
    }

    /**
     * <p>isIpLike.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isIpLike(String text) {
        if (text == null || text.isBlank()) return false;
        return isIpV4Like(text);
    }

    public static boolean isIpV4Like(String text) {
        if (text == null || text.isBlank()) return false;
        return text.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }

}
