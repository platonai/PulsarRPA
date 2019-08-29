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

import com.google.common.net.InetAddresses;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;

import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * A minimal String utility class. Designed for internal dom use only.
 */
public final class StringUtil {

    final public static String[] emptyStringArray = {};
    final public static char COMMA = ',';
    final public static String COMMA_STR = ",";
    final public static char ESCAPE_CHAR = '\\';

    // public static final Pattern HTML_CHARSET_PATTERN = Pattern.compile("^<meta(?!\\s*(?:name|value)\\s*=)(?:[^>]*?content\\s*=[\\s\"']*)?([^>]*?)[\\s\"';]*charset\\s*=[\\s\"']([a-zA-Z0-9]{3,8})([^\\s\"'(/>)]*)", CASE_INSENSITIVE);
    public static final Pattern HTML_CHARSET_PATTERN = Pattern.compile("^<meta.+charset\\s*=[\\s\"']*([a-zA-Z0-9\\-]{3,8})[\\s\"'/>]*", CASE_INSENSITIVE);

    public static final Pattern PRICE_PATTERN = Pattern.compile("[1-9](,{0,1}\\d+){0,8}(\\.\\d{1,2})|[1-9](,{0,1}\\d+){0,8}");

    // all special chars on a standard keyboard
    public static final String DEFAULT_KEEP_CHARS = "~!@#$%^&*()_+`-={}|[]\\:\";'<>?,./' \n\r\t";

    public static final String HTML_TAG_REGEX = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";

    public static final String FLOAT_REGEX = "^([+-]?(\\d+\\.)?\\d+)$";

    public static Pattern FLOAT_PATTERN = Pattern.compile(FLOAT_REGEX);

    public static Pattern HTML_TAG_PATTERN = Pattern.compile(HTML_TAG_REGEX);

    public static final String NUMERIC_LIKE_REGEX = "^.{0,2}[-+]?[0-9]*\\.?[0-9]+.{0,2}$";

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

    public static final Pattern PatternTime = Pattern.compile("[0-2][0-3]:[0-5][0-9]");

    public static int countTimeString(String text) {
        Matcher matcher = PatternTime.matcher(text);
        int count = 0;
        while (matcher.find())
            count++;
        return count;
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

    /**
     * Convenience call for {@link #toHexString(ByteBuffer, String, int)}, where
     * <code>sep = null; lineLen = Integer.MAX_VALUE</code>.
     *
     * @param buf
     */
    public static String toHexString(ByteBuffer buf) {
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
     */
    public static String toHexString(ByteBuffer buf, String sep, int lineLen) {
        return toHexString(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining(), sep, lineLen);
    }

    /**
     * Convenience call for {@link #toHexString(byte[], String, int)}, where
     * <code>sep = null; lineLen = Integer.MAX_VALUE</code>.
     *
     * @param buf
     */
    public static String toHexString(byte[] buf) {
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
     */
    public static String toHexString(byte[] buf, int of, int cb, String sep, int lineLen) {
        if (buf == null)
            return null;
        if (lineLen <= 0)
            lineLen = Integer.MAX_VALUE;
        StringBuffer res = new StringBuffer(cb * 2);
        for (int c = 0; c < cb; c++) {
            int b = buf[of++];
            res.append(HEX_DIGITS[(b >> 4) & 0xf]);
            res.append(HEX_DIGITS[b & 0xf]);
            if (c > 0 && (c % lineLen) == 0)
                res.append('\n');
            else if (sep != null && c < lineLen - 1)
                res.append(sep);
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
        char[] ch = text.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (isChinese(c)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMainlyChinese(String text, double percentage) {
        if ("".equals(text)) return false;

        return 1.0 * countChinese(text) / text.length() >= percentage;
    }

    public static int countChinese(String text) {
        if ("".equals(text)) return 0;

        int count = 0;
        char[] ch = text.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            if (isChinese(ch[i])) {
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
    public static String stripNonChar(String text) {
        return stripNonChar(text, null);
    }

    public static String stripNonChar(String text, String keeps) {
        StringBuilder builder = new StringBuilder();

        if (keeps == null) {
            keeps = "";
        }

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isChineseCharByREG(ch)) {
                builder.append(ch);
            } else if (!keeps.equals("") && keeps.indexOf(ch) != -1) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    public static String trimNonChar(String text) {
        return trimNonChar(text, null);
    }

    // 对字符串的头部和尾部：
    // 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
    // 2. 去除网页空白：&nbsp;
    public static String trimNonChar(String text, String keeps) {
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

        return text.substring(start, end);
    }

    public static boolean isCJK(char ch) {
        return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    }

    // 对整个字符串：
    // 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
    // 2. 去除网页空白：&nbsp;
    //
    // String attrName = "配 送 至：京 东 价：当&nbsp;当&nbsp;价";
    // attrName = StringUtils.strip(attrName).replaceAll("[\\s+:：(&nbsp;)]", "");
    // the "blank" characters in the above phrase can not be stripped
    public static String stripNonCJKChar(String text) {
        return stripNonCJKChar(text, null);
    }

    public static String stripNonCJKChar(String text, String keeps) {
        StringBuilder builder = new StringBuilder();

        if (keeps == null) {
            keeps = "";
        }

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isCJK(ch)) {
                builder.append(ch);
            } else if (!keeps.equals("") && keeps.indexOf(ch) != -1) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    public static String trimNonCJKChar(String text) {
        return trimNonCJKChar(text, null);
    }

    // 对字符串的头部和尾部：
    // 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
    // 2. 去除网页空白：&nbsp;
    public static String trimNonCJKChar(String text, String keeps) {
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

        return text.substring(start, end);
    }

    public static String stripNonPrintableChar(String text) {
        StringBuilder builder = new StringBuilder();

        int len = text.length();
        for (int i = 0; i < len; ++i) {
            char ch = text.charAt(i);
            if (isActuallyWhitespace(ch)) {
                if (i > 0 && i < len - 1) {
                    builder.append(' ');
                }
                int j = i + 1;
                while (j < len && isActuallyWhitespace(text.charAt(j))) {
                    ++j;
                }
                i = j - 1;
            } else if (isPrintableUnicodeChar(ch)) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    /**
     * @link {https://stackoverflow.com/questions/220547/printable-char-in-java}
     */
    public static boolean isPrintableUnicodeChar(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return (!Character.isISOControl(ch)) && ch != KeyEvent.CHAR_UNDEFINED
                && block != null && block != Character.UnicodeBlock.SPECIALS;
    }

    /**
     * Copy from pulsar , remove "�"
     *
     * @param value the dirty String value.
     * @return clean String
     */
    public static String cleanField(String value) {
        value = value.replaceAll("�", "");
        return value;
    }

    public static String getLongestPart(final String text, final Pattern pattern) {
        String[] parts = pattern.split(text);

        if (parts.length == 1) {
            return "";
        }

        String longestPart = "";
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];

            if (p.length() > longestPart.length()) {
                longestPart = p;
            }
        }

        if (longestPart.length() == 0) {
            return "";
        } else {
            return longestPart.trim();
        }
    }

    public static String getLongestPart(final String text, final String regex) {
        return getLongestPart(text, Pattern.compile(regex));
    }

    public static String csslize(String text) {
        text = StringUtils.uncapitalize(text).trim();
        text = StringUtils.join(text.split("(?=\\p{Upper})"), "-").toLowerCase();
        text = text.replaceAll("[-_]+", "-");
        text = text.replaceAll("\\s+", "-");

        return text;
    }

    // nav_top -> nav top, mainMenu -> main menu, image-detail -> image detail
    public static String humanize(String text) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), " ");
        text = text.replaceAll("[-_]", " ").toLowerCase().trim();

        return text;
    }

    public static String humanize(String text, String seperator) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), seperator);
        text = text.replaceAll("[-_]", seperator).toLowerCase().trim();

        return text;
    }

    public static String humanize(String text, String suffix, String seperator) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), seperator);
        text = text.replaceAll("[-_]", seperator).toLowerCase().trim();

        return text + seperator + suffix;
    }

    public static String humanize(Class clazz, String suffix, String seperator) {
        String text = StringUtils.join(clazz.getSimpleName().split("(?=\\p{Upper})"), seperator);
        text = text.replaceAll("[-_]", seperator).toLowerCase().trim();

        return text + seperator + suffix;
    }

    public static int getLeadingInteger(String s, int defaultValue) {
        int numberEnd = StringUtils.lastIndexOfAny(s, "123456789");
        if (numberEnd == StringUtils.INDEX_NOT_FOUND) {
            return defaultValue;
        }
        return NumberUtils.toInt(s.substring(0, numberEnd), defaultValue);
    }

    public static int getTailingInteger(String s, int defaultValue) {
        int numberStart = StringUtils.indexOfAny(s, "123456789");
        if (numberStart == StringUtils.INDEX_NOT_FOUND) {
            return defaultValue;
        }
        return NumberUtils.toInt(s.substring(numberStart), defaultValue);
    }

    public static int getFirstInteger(String s, int defaultValue) {
        int numberStart = StringUtils.indexOfAny(s, "123456789");
        if (numberStart == StringUtils.INDEX_NOT_FOUND) {
            return defaultValue;
        }

        StringBuilder sb = new StringBuilder(s.length() - numberStart);
        int j = 0;
        for (int i = numberStart; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                break;
            }
        }

        return NumberUtils.toInt(sb.toString(), defaultValue);
    }

    public static float getFirstFloatNumber(String s, float defaultValue) {
        Pattern floatNumberPattern = Pattern.compile("[-]?[0-9]*\\.?,?[0-9]+");

        Matcher m = floatNumberPattern.matcher(s);
        if (m.find()) {
            return NumberUtils.toFloat(m.group());
        }

        return defaultValue;
    }

    public static boolean contains(String text, CharSequence... searchCharSequences) {
        Validate.notNull(searchCharSequences);

        for (CharSequence search : searchCharSequences) {
            if (!text.contains(search)) return false;
        }

        return true;
    }

    public static boolean containsAny(String text, CharSequence... searchCharSequences) {
        Validate.notNull(searchCharSequences);

        for (CharSequence search : searchCharSequences) {
            if (text.contains(search)) return true;
        }

        return false;
    }

    public static boolean containsNone(String text, CharSequence... searchCharSequences) {
        Validate.notNull(searchCharSequences);

        for (CharSequence search : searchCharSequences) {
            if (text.contains(search)) return false;
        }

        return true;
    }

    public static String stringifyException(Throwable e) {
        Objects.requireNonNull(e);
        StringWriter stm = new StringWriter();
        PrintWriter wrt = new PrintWriter(stm);
        e.printStackTrace(wrt);
        wrt.close();
        return stm.toString();
    }

    public static String simplifyException(Throwable e) {
        Objects.requireNonNull(e);

        String message = e.getMessage();
        if (message == null) message = e.toString();

        String[] lines = message.split("\n");
        int n = lines.length;
        if (n == 0) {
            return "";
        } else if (n == 1) {
            return lines[0];
        } else if (n == 2) {
            return lines[0] + "\t" + lines[1];
        } else {
            return lines[0] + "\t" + lines[1] + " ...";
        }
    }

    public static String reverse(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        StringBuilder sb = new StringBuilder(s);
        return sb.reverse().toString();
    }

    public static String readableByteCount(long bytes) {
        return readableByteCount(bytes, -1, false);
    }

    public static String readableByteCount(long bytes, boolean si) {
        return readableByteCount(bytes, -1, si);
    }

    public static String readableByteCount(long bytes, int scale, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        String format = scale > 0 ? "%," + scale + ".2f %sB" : "%,.2f %sB";
        return String.format(format, bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Parse key-value pairs in a line, for example :
     * "a=1 b=2 c=3", "x:1 y:2 z:3"
     * @param line The line to parse
     * @return The parsed result
     * */
    public static Map<String, String> parseKvs(String line) {
        return parseKvs(line, "=");
    }

    /**
     * Parse key-value pairs in a line, for example :
     * "a=1 b=2 c=3", "x:1 y:2 z:3"
     * @param line The line to parse
     * @return The parsed result
     * */
    public static Map<String, String> parseKvs(String line, String delimiter) {
        return SParser.wrap(line).getKvs(delimiter);
    }

    /**
     * All lines separated by back slashes are merged together
     * @param allLines All lines separated by "\n", some of them are separated by  back slash
     * @return All lines separated by back slashes are merged
     * */
    public static List<String> getUnslashedLines(String allLines) {
        return mergeSlashedLines(Arrays.asList(allLines.split("\n")));
    }

    public static List<String> getUnslashedLines(String allLines, String EOL) {
        return mergeSlashedLines(Arrays.asList(allLines.split(EOL)));
    }

    /**
     * Merge lines concatenated with back slash, for example :
     * The line "Java Stream API is very very usefull \\\n"
     * " and we use them everywhere "
     * can be merged into "Java Stream API is very very usefull and we use them everywhere"
     *
     * */
    public static String mergeSlashedLine(String slashedLine) {
        slashedLine = slashedLine.replaceAll("\n", "");
        slashedLine = slashedLine.replaceAll("\\\\", "");
        return slashedLine;
    }

    /**
     * All lines separated by back slashes are merged together
     * @param linesWithSlash Lines with back slash
     * @return All lines separated by back slashes are merged
     * */
    public static List<String> mergeSlashedLines(Iterable<String> linesWithSlash) {
        List<String> lines = new ArrayList<>();
        StringBuilder mergedLine = new StringBuilder();
        boolean merging;
        for (String line : linesWithSlash) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.endsWith("\\")) {
                merging = true;
                mergedLine.append(StringUtils.removeEnd(line, "\\"));
            } else {
                mergedLine.append(line);
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

    public static int getLongestCommonSubstring(String a, String b){
        int m = a.length();
        int n = b.length();

        int max = 0;

        int[][] dp = new int[m][n];

        for(int i=0; i<m; i++){
            for(int j=0; j<n; j++){
                if(a.charAt(i) == b.charAt(j)){
                    if(i==0 || j==0){
                        dp[i][j]=1;
                    }else{
                        dp[i][j] = dp[i-1][j-1]+1;
                    }

                    if(max < dp[i][j])
                        max = dp[i][j];
                }

            }
        }

        return max;
    }

    /**
     * Not completely tested
     * */
    public static String replaceCharsetInHtml(String html, String charset) {
        Matcher matcher = HTML_CHARSET_PATTERN.matcher(html);
        if (matcher.find()) {
            html = html.replaceAll(matcher.group(1), charset);
        }

        return html;
    }


    /**
     * Returns an arraylist of strings.
     * @param str the comma seperated string values
     * @return the arraylist of the comma seperated string values
     */
    public static String[] getStrings(String str){
        Collection<String> values = getStringCollection(str);
        if(values.size() == 0) {
            return null;
        }
        return values.toArray(new String[0]);
    }

    public static String arrayToString(String[] strs) {
        if (strs.length == 0) { return ""; }
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(strs[0]);
        for (int idx = 1; idx < strs.length; idx++) {
            sbuf.append(",");
            sbuf.append(strs[idx]);
        }
        return sbuf.toString();
    }

    /**
     * Returns a collection of strings.
     * @param str comma seperated string values
     * @return an <code>ArrayList</code> of string values
     */
    public static Collection<String> getStringCollection(String str){
        String delim = ",";
        return getStringCollection(str, delim);
    }

    /**
     * Returns a collection of strings.
     *
     * @param str
     *          String to parse
     * @param delim
     *          delimiter to separate the values
     * @return Collection of parsed elements.
     */
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
     * @param str a comma separated <String> with values
     * @return a <code>Collection</code> of <code>String</code> values
     */
    public static Collection<String> getTrimmedStringCollection(String str){
        Set<String> set = new LinkedHashSet<String>(Arrays.asList(getTrimmedStrings(str)));
        set.remove("");
        return set;
    }

    /**
     * Splits a comma separated value <code>String</code>, trimming leading and trailing whitespace on each value.
     * @param str a comma separated <String> with values
     * @return an array of <code>String</code> values
     */
    public static String[] getTrimmedStrings(String str){
        if (null == str || str.trim().isEmpty()) {
            return emptyStringArray;
        }

        return str.trim().split("\\s*,\\s*");
    }

    public static boolean hasHTMLTags(String text) {
        Matcher matcher = HTML_TAG_PATTERN.matcher(text);
        return matcher.find();
    }

    public static boolean isFloat(String text) {
        return FLOAT_PATTERN.matcher(text).matches();
    }

    public static boolean isNumericLike(String text) {
        return NUMERIC_LIKE_PATTERN.matcher(text).matches();
    }

    /**
     * https://www.regextester.com/97725
     * */
    public static boolean isMoneyLike(String text) {
        return MONEY_LIKE_PATTERN.matcher(text).matches();
    }

    public static boolean isIpPortLike(String text) {
        return IP_PORT_PATTERN.matcher(text).matches();
    }

    public static boolean isIpLike(String text) {
        return text.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }

    /**
     * The traditional binary prefixes, kilo, mega, ..., exa,
     * which can be represented by a 64-bit integer.
     * TraditionalBinaryPrefix symbol are case insensitive.
     */
    public enum TraditionalBinaryPrefix {
        KILO(10),
        MEGA(KILO.bitShift + 10),
        GIGA(MEGA.bitShift + 10),
        TERA(GIGA.bitShift + 10),
        PETA(TERA.bitShift + 10),
        EXA (PETA.bitShift + 10);

        public final long value;
        public final char symbol;
        public final int bitShift;
        public final long bitMask;

        TraditionalBinaryPrefix(int bitShift) {
            this.bitShift = bitShift;
            this.value = 1L << bitShift;
            this.bitMask = this.value - 1L;
            this.symbol = toString().charAt(0);
        }

        /**
         * @return The TraditionalBinaryPrefix object corresponding to the symbol.
         */
        public static TraditionalBinaryPrefix valueOf(char symbol) {
            symbol = Character.toUpperCase(symbol);
            for(TraditionalBinaryPrefix prefix : TraditionalBinaryPrefix.values()) {
                if (symbol == prefix.symbol) {
                    return prefix;
                }
            }
            throw new IllegalArgumentException("Unknown symbol '" + symbol + "'");
        }

        /**
         * Convert a string to long.
         * The input string is first be trimmed
         * and then it is parsed with traditional binary prefix.
         *
         * For example,
         * "-1230k" will be converted to -1230 * 1024 = -1259520;
         * "891g" will be converted to 891 * 1024^3 = 956703965184;
         *
         * @param s input string
         * @return a long value represented by the input string.
         */
        public static long string2long(String s) {
            s = s.trim();
            final int lastpos = s.length() - 1;
            final char lastchar = s.charAt(lastpos);
            if (Character.isDigit(lastchar))
                return Long.parseLong(s);
            else {
                long prefix;
                try {
                    prefix = TraditionalBinaryPrefix.valueOf(lastchar).value;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid size prefix '" + lastchar
                            + "' in '" + s
                            + "'. Allowed prefixes are k, m, g, t, p, e(case insensitive)");
                }
                long num = Long.parseLong(s.substring(0, lastpos));
                if (num > (Long.MAX_VALUE/prefix) || num < (Long.MIN_VALUE/prefix)) {
                    throw new IllegalArgumentException(s + " does not fit in a Long");
                }
                return num * prefix;
            }
        }

        /**
         * Convert a long integer to a string with traditional binary prefix.
         *
         * @param n the value to be converted
         * @param unit The unit, e.g. "B" for bytes.
         * @param decimalPlaces The number of decimal places.
         * @return a string with traditional binary prefix.
         */
        public static String long2String(long n, String unit, int decimalPlaces) {
            if (unit == null) {
                unit = "";
            }
            //take care a special case
            if (n == Long.MIN_VALUE) {
                return "-8 " + EXA.symbol + unit;
            }

            final StringBuilder b = new StringBuilder();
            //take care negative numbers
            if (n < 0) {
                b.append('-');
                n = -n;
            }
            if (n < KILO.value) {
                //no prefix
                b.append(n);
                return (unit.isEmpty()? b: b.append(" ").append(unit)).toString();
            } else {
                //find traditional binary prefix
                int i = 0;
                for(; i < values().length && n >= values()[i].value; i++);
                TraditionalBinaryPrefix prefix = values()[i - 1];

                if ((n & prefix.bitMask) == 0) {
                    //exact division
                    b.append(n >> prefix.bitShift);
                } else {
                    final String  format = "%." + decimalPlaces + "f";
                    String s = format(format, n/(double)prefix.value);
                    //check a special rounding up case
                    if (s.startsWith("1024")) {
                        prefix = values()[i];
                        s = format(format, n/(double)prefix.value);
                    }
                    b.append(s);
                }
                return b.append(' ').append(prefix.symbol).append(unit).toString();
            }
        }
    }

    /** The same as String.format(Locale.ENGLISH, format, objects). */
    private static String format(final String format, final Object... objects) {
        return String.format(Locale.ENGLISH, format, objects);
    }
}
