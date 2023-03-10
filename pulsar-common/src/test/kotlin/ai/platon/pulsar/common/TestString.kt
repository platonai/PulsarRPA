/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for StringUtil methods.
 */
class TestString {
    var conf = ImmutableConfig()
    @Test
    fun testValueOf() {
        val i = 18760506
        assertEquals("18760506", i.toString())
    }

    @Test
    fun testSubstring() {
        var s = "a,b,c,d"
        s = StringUtils.substringAfter(s, ",")
        assertEquals("b,c,d", s)
        s = "a\nb\nc\nd\ne\nf\ng\n"
        // assume the avarage lenght of a link is 100 characters
        val pos = StringUtils.indexOf(s, '\n'.toInt(), s.length / 2)
        s = s.substring(pos + 1)
        assertEquals("e\nf\ng\n", s)
    }

    @Test
    fun testToHexString() {
        val buffer = ByteBuffer.wrap("".toByteArray())
        assertEquals("", Strings.toHexString(buffer))
    }

    @Test
    fun testPadding() {
        val strings = arrayOf(
                "1.\thttp://v.ifeng.com/\t凤凰视频首页-最具媒体价值的视频门户-凤凰网",
                "2.\thttp://fo.ifeng.com/\t佛教首页_佛教频道__凤凰网",
                "3.\thttp://www.ifeng.com/\t凤凰网",
                "24.\thttp://fashion.ifeng.com/health/\t凤凰健康_关注全球华人健康"
        )
        IntStream.range(0, strings.size).forEach { i: Int -> strings[i] = StringUtils.rightPad(strings[i], 60) }
        Stream.of(*strings).forEach { x: String? -> println(x) }
    }

    @Test
    fun testRegex() {
        var text = "http://aitxt.com/book/12313413874"
        var regex = "http://(.*)aitxt.com(.*)"
        assertTrue(text.matches(regex.toRegex()))
        text = "http://aitxt.com/book/12313413874"
        regex = ".*"
        assertTrue(text.matches(regex.toRegex()))
        regex = "aitxt"
        assertFalse(text.matches(regex.toRegex()))
        text = "abcde"
        regex = "[a-zA-Z](?!\\d+).+"
        assertTrue(text.matches(regex.toRegex()))
        text = "ab12212cde"
        regex = "ab\\d+.+"
        assertTrue(text.matches(regex.toRegex()))
        regex = "(>|<|>=|<=)*([*\\d+]),(>|<|>=|<=)*([*\\d+]),(>|<|>=|<=)*([*\\d+]),(>|<|>=|<=)*([*\\d+])"
        assertTrue(">1,2,3,4".matches(regex.toRegex()))
        assertTrue(">=1,*,3,4".matches(regex.toRegex()))
        assertTrue("1,2,*,4".matches(regex.toRegex()))
        assertTrue("1,*,*,*".matches(regex.toRegex()))
        var PATTERN_RECT = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        text = ">=1,*,3,4"
        assertTrue(PATTERN_RECT.matcher(text).matches())
        val matcher: Matcher
        //        matcher = PATTERN_RECT.matcher(text);
//        if (matcher.find()) {
//          System.out.println(matcher.group(0));
//          System.out.println(matcher.group(1) + matcher.group(2));
//          System.out.println(matcher.group(3) + matcher.group(4));
//          System.out.println(matcher.group(5) + matcher.group(6));
//          System.out.println(matcher.group(7) + matcher.group(8));
//        }
        text = "*,*,230,420"
        val REGEX_RECT = "([+-])?(\\*|\\d+),([+-])?(\\*|\\d+),([+-])?(\\*|\\d+),([+-])?(\\*|\\d+)"
        PATTERN_RECT = Pattern.compile(REGEX_RECT, Pattern.CASE_INSENSITIVE)
        matcher = PATTERN_RECT.matcher(text)
        if (matcher.find()) {
            println(matcher.group(0))
            println(matcher.group(1) + matcher.group(2))
            println(matcher.group(3) + matcher.group(4))
            println(matcher.group(5) + matcher.group(6))
            println(matcher.group(7) + matcher.group(8))
        }
    }

    @Test
    fun testRegex2() {
        val url = "https://www.amazon.com/s?k=insomnia&i=aps&page=100"
        val regex = ".+&i=.+".toRegex()
        assertTrue { url.matches(regex) }

        assertEquals("a.b", "a..b".replace("\\.+".toRegex(), "."))

        assertTrue { "about:blank".matches("about:.+".toRegex()) }
    }

    @Test
    fun testRegex3() {
        val text = """
            |xvfb-run -a -e /dev/stdout -s "-screen 0 1920x1080x24" /usr/bin/google-chrome-stable 
            |--proxy-server=119.49.122.242:4224 --disable-gpu --hide-scrollbars --remote-debugging-port=0 
            |--no-default-browser-check --no-first-run --no-startup-window --mute-audio 
            |--disable-background-networking --disable-background-timer-throttling 
            |--disable-client-side-phishing-detection --disable-hang-monitor 
            |--disable-popup-blocking --disable-prompt-on-repost --disable-sync --disable-translate 
            |--disable-blink-features=AutomationControlled --metrics-recording-only 
            |--safebrowsing-disable-auto-update --no-sandbox --ignore-certificate-errors 
            |--window-size=1920,1080 --pageLoadStrategy=none --throwExceptionOnScriptError=true 
            |--user-data-dir=/home/vincent/tmp/pulsar-vincent/context/cx.2zmmAe40/pulsar_chrome
        """.trimMargin().replace("\n", " ")
        // println(text)
        assertTrue { "./pulsar-vincent/context/cx.5oruW037".matches(".+pulsar-.+/context/cx.+".toRegex()) }
        assertTrue { text.matches(".+pulsar-.+/context/cx.+".toRegex()) }
    }

    @Test
    fun testReplaceCharsetInHtml() {
        val lines = ResourceLoader.readAllLines("data/html-charsets.txt")
        for (line in lines) {
            val l = Strings.replaceCharsetInHtml(line, "UTF-8")
            assertTrue(l.contains("UTF-8"))
        }
    }

    @Test
    fun testAvailableCharsets() {
        var charsets = Charset.availableCharsets().values.stream()
                .map { obj: Charset -> obj.name() }
                .collect(Collectors.joining("|"))
        var charsetPattern = Pattern.compile(charsets, Pattern.CASE_INSENSITIVE)
        assertEquals(Charset.availableCharsets().size.toLong(), StringUtils.countMatches(charsets, "|") + 1.toLong())
        assertTrue(charsetPattern.matcher("gb2312").matches())
        assertTrue(charsetPattern.matcher("UTF-8").matches())
        assertTrue(charsetPattern.matcher("windows-1257").matches())
        charsets = ("UTF-8|GB2312|GB18030|GBK|Big5|ISO-8859-1"
                + "|windows-1250|windows-1251|windows-1252|windows-1253|windows-1254|windows-1257"
                + "|UTF-8")
        charsets = charsets.replace("UTF-8\\|?".toRegex(), "")
        charsetPattern = Pattern.compile(charsets, Pattern.CASE_INSENSITIVE)
        assertTrue(charsetPattern.matcher("gb2312").matches())
        assertTrue(charsetPattern.matcher("windows-1257").matches())
        assertFalse(charsetPattern.matcher("UTF-8").matches())
        assertFalse(charsetPattern.matcher("nonsense").matches())
    }

    @Test
    fun testPricePattern() {
        val text = "￥799.00 (降价通知)"
        // text = text.replaceAll("¥|,|'", "");
// System.out.println(text);
        val matcher = Strings.PRICE_PATTERN.matcher(text)
        var count = 0
        while (matcher.find()) {
            count++
            // System.out.println("Found Price : " + count + " : " + matcher.start() + " - " + matcher.end() + ", " + matcher.group());
        }
        assertTrue(count > 0)
    }

    @Test
    fun testParseVersion() {
        // assertTrue(Math.abs(StringUtil.tryParseDouble("0.2.0") - 0.2) < 0.0000001);
//    System.out.println(Lists.newArrayList("0.2.0".split("\\.")));
//    System.out.println("0.2.0".split("\\.").length);
        assertEquals(3, "0.2.0".split(".").size)
    }

    @Test
    fun testTrim() {
        var text = " 个性表盘  "
        assertTrue { ' '.isWhitespace() }
        // String.trim() == CharSequence.trim(Char::isWhitespace)
        assertEquals("个性表盘", text.trim())
        assertEquals("个性表盘  ", text.trim { it <= ' ' })
    }

    @Test
    fun testTrimNonCJKChar() {
        val text = "天王表 正品热卖  个性表盘  "
        assertEquals("天王表 正品热卖  个性表盘  ", text.trim { it <= ' ' })
        assertEquals("天王表 正品热卖  个性表盘", Strings.trimNonCJKChar(text))
    }

    @Test
    fun testStripNonChar() {
        val texts = arrayOf(
                "天王表 正品热卖  个性表盘  ",
                "天王表 正品热卖 \uE004主要职责：  OK"
        )
        for (text in texts) {
            println(Strings.stripNonCJKChar(text, Strings.DEFAULT_KEEP_CHARS))
        }
    }

    @Test
    fun testIsChinese() {
        val texts = arrayOf(
                "关注全球华人健康"
        )
        for (text in texts) {
            assertTrue(Strings.isChinese(text))
        }
        val noChineseTexts = arrayOf(
                "1234534",
                "alphabetical",
                "1b关注全球华人健康",
                "a关注全球华人健康"
        )
        for (text in noChineseTexts) { // TODO: noChineseTexts assertion failed
// assertFalse(text, StringUtil.isChinese(text));
        }
        val mainlyChineseTexts = arrayOf(
                "1234关注全球华人健康关注全球华人健康",
                "alpha关注全球华人健康关注全球华人健康",
                "1b关注全球华人健康",
                "a关注全球华人健康"
        )

        for (text in mainlyChineseTexts) {
            println(Strings.countChinese(text).toString() + "/" + text.length
                    + "=" + Strings.countChinese(text) * 1.0 / text.length + "\t" + text)
            assertTrue(Strings.isMainlyChinese(text, 0.6), text)
        }
    }

    @Test
    fun testStringFormat() {
        println(String.format("%1$,20d", -3123))
        println(String.format("%1$9d", -31))
        println(String.format("%1$-9d", -31))
        println(String.format("%1$(9d", -31))
        println(String.format("%1$#9x", 5689))
    }

    @Test
    fun testRegexSplit() {
        var s = "TestStringUtil"
        val r = s.split("(?=\\p{Upper})".toRegex()).filterNot { it.isEmpty() }.toTypedArray()
        assertArrayEquals(arrayOf("Test", "String", "Util"), r)
        assertEquals("Test.String.Util", StringUtils.join(r, "."))
        var url = "http://t.tt/\t-i 1m"
        var parts = StringUtils.split(url, "\t")
        assertEquals("http://t.tt/", parts[0])
        assertEquals("-i 1m", parts[1])
        url = "http://t.tt/"
        parts = StringUtils.split(url, "\t")
        assertEquals("http://t.tt/", parts[0])
        s = "ld,-o,-s,-w:hello,-a:b,-c"
        val options = StringUtils.replaceChars(s, ":,", Strings.padding[2]).split(" ").toTypedArray()
        println(StringUtils.join(options, " "))
    }

    @Test
    fun testCsslize() {
        var s = "-TestStringUtil"
        assertEquals("-test-string-util", Strings.csslize(s))
        s = "TestStringUtil-a"
        assertEquals("test-string-util-a", Strings.csslize(s))
        s = "TestStringUtil-"
        assertEquals("test-string-util-", Strings.csslize(s))
    }

    @Test
    fun testCsslize2() {
        val cases: MutableMap<String, String> = HashMap()
        cases["nav_top"] = "nav-top"
        cases["mainMenu"] = "main-menu"
        cases["image-detail"] = "image-detail"
        cases["image      detail"] = "image-detail"
        for ((key, value) in cases) {
            assertEquals(value, Strings.csslize(key))
        }
    }

    @Test
    fun testHumanize() {
        val s = "TestStringUtil"
        assertEquals("test string util", Strings.humanize(s))
        assertEquals("test.string.util", Strings.humanize(s, "."))
    }

    @Test
    fun testParseKvs() {
        val kvs: MutableMap<String, String> = HashMap()
        kvs["a"] = "1"
        kvs["b"] = "2"
        kvs["c"] = "3"
        assertEquals(kvs, Strings.parseKvs("a=1 b=2 c=3"))
        assertEquals(kvs, Strings.parseKvs("a:1\nb:2\tc:3", ":"))
        assertTrue(Strings.parseKvs("abcd1234*&#$").isEmpty())
        println(Strings.parseKvs("a=1 b=2 c=3 c=4  d e f"))
        println(SParser.wrap("a=1 b=2 c=3,c=4 d e f").getKvs("="))
        println(SParser.wrap("a=1 b=2 c=3 c=4,d= e f").getKvs("="))
        println(SParser.wrap("").kvs)
        val kvs2 = arrayOf(
                "a=1 b=2 c=3,c=4 d e f",
                "a=1 b=2 c=3 c=4 d= e f",
                "a=1,b=2,c=3,c=4,d= e f.3     ",
                "   a=1     b=2\tc=3 c=4 d= e =3     ")
        for (i in kvs2.indices) {
            val kv = SParser.wrap(kvs2[i]).getKvs("=")
            assertEquals("{a=1, b=2, c=4}", kv.toString(), i.toString() + "th [" + kvs2[i] + "]")
        }
    }

    @Test
    fun testGetUnslashedLines() {
        var s = "http://www.sxrb.com/sxxww/\t--fetch-interval=1s --fetch-priority=1010 \\\n" +
                "    --follow-dom=:root --follow-url=.+ --follow-anchor=8,40 \\\n" +
                "    --entity=#content --entity-fields=title:#title,content:#content,publish_time:#publish_time \\\n" +
                "    --collection=#comments --collection-item=.comment --collection-item-fields=publish_time:.comment_publish_time,author:.author,content:.content\n" +
                "http://news.qq.com/\t--fetch-interval=1h --entity=#content\n" +
                "http://news.youth.cn/\t--fetch-interval=1h --entity=#content\\\n" +
                "    --collection=#comments\n" +
                "http://news.163.com/\t--fetch-interval=1h --entity=#content" +
                "\n"
        var lines = Strings.getUnslashedLines(s)
        assertEquals(4, lines.size.toLong())
        s = "http://sz.sxrb.com/sxxww/dspd/szpd/bwch/\n" +
                "http://sz.sxrb.com/sxxww/dspd/szpd/fcjjjc/\n" +
                "http://sz.sxrb.com/sxxww/dspd/szpd/hydt/"
        lines = Strings.getUnslashedLines(s)
        assertEquals(3, lines.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testLoadSlashedLines() {
        var seeds = "@data/lines-with-slashes.txt"
        if (seeds.startsWith("@")) {
            seeds = ResourceLoader.readAllLines(seeds.substring(1)).joinToString("\n")
        }
        val seedFile = File.createTempFile("seed", ".txt")
        val unslashedLines = Strings.getUnslashedLines(seeds)
        assertEquals(111, unslashedLines.size.toLong())
        FileUtils.writeLines(seedFile, unslashedLines)
        assertEquals(111, Files.readAllLines(seedFile.toPath()).size.toLong())
    }

    @Test
    fun testGetFirstInteger() {
        val texts = arrayOf(
                "-hello world 999 i love you 520 forever",
                "i have received $1964,234 last day",
                "this is a java number: 1_435_324"
        )
        val expects = arrayOf(999, 1964_234, 1_435_324)

        IntRange(0, texts.size - 1).forEach { i ->
            assertEquals(expects[i], Strings.getFirstInteger(texts[i], 0), "The $i-th test is failed")
        }

        val url = "https://www.amazon.com/s?k=insomnia&i=todays-deals&bbn=21101958011&page=2&qid=1609866830&ref=sr_pg_1"
        assertEquals(2, Strings.getFirstInteger(url.substringAfter("page="), 0))
    }

    @Test
    fun testGetLastInteger() {
        val texts = arrayOf(
                "-hello world 999 i love you 520 forever",
                "i have received $1964,234 yesterday, and $2046,123 the day before yesterday",
                "this is a java number: 1_435_324, and this is another: 2_457_325"
        )
        val expects = arrayOf(520, 2046_123, 2_457_325)

        IntRange(0, texts.size - 1).forEach { i ->
            assertEquals(expects[i], Strings.getLastInteger(texts[i], 0), "The $i-th test is failed")
        }

        assertEquals(150, Strings.getLastInteger("631 global ratings | 150 global reviews", 0))
    }

    @Test
    fun testGetFirstFloatNumber() {
        val texts = arrayOf(
                "-hello world 999.234 i love you 520.02 forever",
                "i have received $1964,234.1 last day",
                "this is a java number: 1_435_324.92"
        )
        val expects = arrayOf(999.234f, 1964_234.1f, 1_435_324.92f)

        IntRange(0, texts.size - 1).forEach { i ->
            assertEquals(expects[i], Strings.getFirstFloatNumber(texts[i], Float.MIN_VALUE), "The $i-th test is failed")
        }
    }

    @Test
    fun testHashCode() {
        val s = "https://www.amazon.com/s?k=insomnia&i=aps&page=15"
        val s2 = "https://www.amazon.com/s?k=insomnia&i=aps&page=16"
        val s3 = "https://www.amazon.com/s?k=insomnia&i=aps&page=17"
        assertNotEquals(s.hashCode(), s2.hashCode())
        assertNotEquals(s.hashCode(), s3.hashCode())
        assertNotEquals(s2.hashCode(), s3.hashCode())
    }
}
