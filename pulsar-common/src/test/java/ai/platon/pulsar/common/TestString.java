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

import ai.platon.pulsar.common.config.ImmutableConfig;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Unit tests for StringUtil methods.
 */
public class TestString {

    ImmutableConfig conf = new ImmutableConfig();

    @Test
    public void testValueOf() {
        int i = 18760506;
        assertEquals("18760506", String.valueOf(i));
    }

    @Test
    public void testSubstring() {
        String s = "a,b,c,d";
        s = StringUtils.substringAfter(s, ",");
        assertEquals("b,c,d", s);

        s = "a\nb\nc\nd\ne\nf\ng\n";
        // assume the avarage lenght of a link is 100 characters
        int pos = StringUtils.indexOf(s, '\n', s.length() / 2);
        s = s.substring(pos + 1);
        assertEquals("e\nf\ng\n", s);
    }

    @Test
    public void testToHexString() {
        ByteBuffer buffer = ByteBuffer.wrap("".getBytes());
        assertEquals("", StringUtil.toHexString(buffer));
    }

    @Test
    public void testPadding() {
        String[] strings = {
                "1.\thttp://v.ifeng.com/\t凤凰视频首页-最具媒体价值的视频门户-凤凰网",
                "2.\thttp://fo.ifeng.com/\t佛教首页_佛教频道__凤凰网",
                "3.\thttp://www.ifeng.com/\t凤凰网",
                "24.\thttp://fashion.ifeng.com/health/\t凤凰健康_关注全球华人健康"
        };
        IntStream.range(0, strings.length).forEach(i -> strings[i] = StringUtils.rightPad(strings[i], 60));
        Stream.of(strings).forEach(System.out::println);
    }

    @Test
    public void testRegex() {
        String text = "http://aitxt.com/book/12313413874";
        String regex = "http://(.*)aitxt.com(.*)";
        assertTrue(text.matches(regex));

        text = "http://aitxt.com/book/12313413874";
        regex = ".*";
        assertTrue(text.matches(regex));

        regex = "aitxt";
        assertFalse(text.matches(regex));

        text = "abcde";
        regex = "[a-zA-Z](?!\\d+).+";
        assertTrue(text.matches(regex));

        text = "ab12212cde";
        regex = "ab\\d+.+";
        assertTrue(text.matches(regex));

        regex = "(>|<|>=|<=)*([*\\d+]),(>|<|>=|<=)*([*\\d+]),(>|<|>=|<=)*([*\\d+]),(>|<|>=|<=)*([*\\d+])";
        assertTrue(">1,2,3,4".matches(regex));
        assertTrue(">=1,*,3,4".matches(regex));
        assertTrue("1,2,*,4".matches(regex));
        assertTrue("1,*,*,*".matches(regex));

        Pattern PATTERN_RECT = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        text = ">=1,*,3,4";
        assertTrue(PATTERN_RECT.matcher(text).matches());

        Matcher matcher;
//        matcher = PATTERN_RECT.matcher(text);
//        if (matcher.find()) {
//          System.out.println(matcher.group(0));
//          System.out.println(matcher.group(1) + matcher.group(2));
//          System.out.println(matcher.group(3) + matcher.group(4));
//          System.out.println(matcher.group(5) + matcher.group(6));
//          System.out.println(matcher.group(7) + matcher.group(8));
//        }

        text = "*,*,230,420";
        String REGEX_RECT = "([+-])?(\\*|\\d+),([+-])?(\\*|\\d+),([+-])?(\\*|\\d+),([+-])?(\\*|\\d+)";
        PATTERN_RECT = Pattern.compile(REGEX_RECT, Pattern.CASE_INSENSITIVE);
        matcher = PATTERN_RECT.matcher(text);
        if (matcher.find()) {
            System.out.println(matcher.group(0));
            System.out.println(matcher.group(1) + matcher.group(2));
            System.out.println(matcher.group(3) + matcher.group(4));
            System.out.println(matcher.group(5) + matcher.group(6));
            System.out.println(matcher.group(7) + matcher.group(8));
        }
    }

    @Test
    public void testReplaceCharsetInHtml() {
        List<String> lines = ResourceLoader.readAllLines("data/html-charsets.txt");
        for (String line : lines) {
            line = StringUtil.replaceCharsetInHtml(line, "UTF-8");
            assertTrue(line, line.contains("UTF-8"));
        }
    }

    @Test
    public void testAvailableCharsets() {
        String charsets = Charset.availableCharsets().values().stream()
                .map(Charset::name)
                .collect(Collectors.joining("|"));

        Pattern charsetPattern = Pattern.compile(charsets, Pattern.CASE_INSENSITIVE);
        assertEquals(Charset.availableCharsets().size(), StringUtils.countMatches(charsets, "|") + 1);
        assertTrue(charsetPattern.matcher("gb2312").matches());
        assertTrue(charsetPattern.matcher("UTF-8").matches());
        assertTrue(charsetPattern.matcher("windows-1257").matches());

        charsets = "UTF-8|GB2312|GB18030|GBK|Big5|ISO-8859-1"
                + "|windows-1250|windows-1251|windows-1252|windows-1253|windows-1254|windows-1257"
                + "|UTF-8";
        charsets = charsets.replaceAll("UTF-8\\|?", "");
        charsetPattern = Pattern.compile(charsets, Pattern.CASE_INSENSITIVE);
        assertTrue(charsetPattern.matcher("gb2312").matches());
        assertTrue(charsetPattern.matcher("windows-1257").matches());
        assertFalse(charsetPattern.matcher("UTF-8").matches());
        assertFalse(charsetPattern.matcher("nonsense").matches());
    }

    @Test
    public void testPricePattern() {
        String text = "￥799.00 (降价通知)";
        // text = text.replaceAll("¥|,|'", "");
        // System.out.println(text);

        Matcher matcher = StringUtil.PRICE_PATTERN.matcher(text);

        int count = 0;
        while (matcher.find()) {
            count++;
            // System.out.println("Found Price : " + count + " : " + matcher.start() + " - " + matcher.end() + ", " + matcher.group());
        }

        assertTrue(count > 0);
    }

    @Test
    public void testParseVersion() {
        // assertTrue(Math.abs(StringUtil.tryParseDouble("0.2.0") - 0.2) < 0.0000001);
//    System.out.println(Lists.newArrayList("0.2.0".split("\\.")));
//    System.out.println("0.2.0".split("\\.").length);
        assertEquals("0.2.0".split("\\.").length, 3);
    }

    @Test
    public void testtrimNonCJKChar() {
        String text = "天王表 正品热卖 机械表 全自动 男士商务气派钢带手表GS5733T/D尊贵大气 个性表盘  ";

        assertEquals(text.trim(), "天王表 正品热卖 机械表 全自动 男士商务气派钢带手表GS5733T/D尊贵大气 个性表盘  ");
        assertEquals(StringUtil.trimNonCJKChar(text), "天王表 正品热卖 机械表 全自动 男士商务气派钢带手表GS5733T/D尊贵大气 个性表盘");
    }

    @Test
    public void testStripNonChar() {
        String[] texts = {
                "天王表 正品热卖 机械表 全自动 男士商务气派钢带手表GS5733T/D尊贵大气 个性表盘  ",
                "天王表 正品热卖 \uE004主要职责：  OK"
        };

        for (String text : texts) {
            System.out.println(StringUtil.stripNonCJKChar(text, StringUtil.DEFAULT_KEEP_CHARS));
        }

//    assertEquals(text.trim(), "天王表 正品热卖 机械表 全自动 男士商务气派钢带手表GS5733T/D尊贵大气 个性表盘  ");
//    assertEquals(StringUtil.stripNonCJKChar(text), "天王表 正品热卖 机械表 全自动 男士商务气派钢带手表GS5733T/D尊贵大气 个性表盘");
    }

    @Test
    public void testIsChinese() {
        String[] texts = {
                "关注全球华人健康"
        };

        for (String text : texts) {
            assertTrue(StringUtil.isChinese(text));
        }

        String[] noChineseTexts = {
                "1234534",
                "alphabetical",
                "1b关注全球华人健康",
                "a关注全球华人健康"
        };

        for (String text : noChineseTexts) {
            // TODO: noChineseTexts assertion failed
            // assertFalse(text, StringUtil.isChinese(text));
        }

        String[] mainlyChineseTexts = {
                "1234关注全球华人健康关注全球华人健康",
                "alpha关注全球华人健康关注全球华人健康",
                "1b关注全球华人健康",
                "a关注全球华人健康"
        };

        for (String text : mainlyChineseTexts) {
            System.out.println(StringUtil.countChinese(text) + "/" + text.length()
                    + "=" + StringUtil.countChinese(text) * 1.0 / text.length() + "\t" + text);
            assertTrue(text, StringUtil.isMainlyChinese(text, 0.6));
        }
    }

    @Test
    public void testStringFormat() {
        System.out.println(String.format("%1$,20d", -3123));
        System.out.println(String.format("%1$9d", -31));
        System.out.println(String.format("%1$-9d", -31));
        System.out.println(String.format("%1$(9d", -31));
        System.out.println(String.format("%1$#9x", 5689));
    }

    @Test
    public void testSplit() {
        String s = "TestStringUtil";
        String[] r = s.split("(?=\\p{Upper})");
        assertArrayEquals(new String[]{"Test", "String", "Util"}, r);
        assertEquals("Test.String.Util", StringUtils.join(r, "."));

        String url = "http://t.tt/\t-i 1m";
        String[] parts = StringUtils.split(url, "\t");
        assertEquals("http://t.tt/", parts[0]);
        assertEquals("-i 1m", parts[1]);

        url = "http://t.tt/";
        parts = StringUtils.split(url, "\t");
        assertEquals("http://t.tt/", parts[0]);

        s = "ld,-o,-s,-w:hello,-a:b,-c";
        String[] options = StringUtils.replaceChars(s, ":,", StringUtil.padding[2]).split(" ");
        System.out.println(StringUtils.join(options, " "));
    }

    @Test
    public void testCsslize() {
        String s = "-TestStringUtil";
        assertEquals("-test-string-util", StringUtil.csslize(s));

        s = "TestStringUtil-a";
        assertEquals("test-string-util-a", StringUtil.csslize(s));

        s = "TestStringUtil-";
        assertEquals("test-string-util-", StringUtil.csslize(s));
    }

    @Test
    public void testCsslize2() {
        Map<String, String> cases = new HashMap<>();

        cases.put("nav_top", "nav-top");
        cases.put("mainMenu", "main-menu");
        cases.put("image-detail", "image-detail");
        cases.put("image      detail", "image-detail");

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            assertEquals(entry.getValue(), StringUtil.csslize(entry.getKey()));
        }
    }

    @Test
    public void testHumanize() {
        String s = "TestStringUtil";
        assertEquals("test string util", StringUtil.humanize(s));
        assertEquals("test.string.util", StringUtil.humanize(s, "."));
    }

    @Test
    public void testParseKvs() {
        Map<String, String> kvs = new HashMap<>();
        kvs.put("a", "1");
        kvs.put("b", "2");
        kvs.put("c", "3");

        assertEquals(kvs, StringUtil.parseKvs("a=1 b=2 c=3"));
        assertEquals(kvs, StringUtil.parseKvs("a:1\nb:2\tc:3", ":"));
        assertTrue(StringUtil.parseKvs("abcd1234*&#$").isEmpty());

        System.out.println(StringUtil.parseKvs("a=1 b=2 c=3 c=4  d e f"));
        System.out.println(SParser.wrap("a=1 b=2 c=3,c=4 d e f").getKvs("="));
        System.out.println(SParser.wrap("a=1 b=2 c=3 c=4,d= e f").getKvs("="));

        System.out.println(SParser.wrap("").getKvs());

        String[] kvs2 = {
                "a=1 b=2 c=3,c=4 d e f",
                "a=1 b=2 c=3 c=4 d= e f",
                "a=1,b=2,c=3,c=4,d= e f.3     ",
                "   a=1     b=2\tc=3 c=4 d= e =3     ",
        };
        for (int i = 0; i < kvs2.length; ++i) {
            Map<String, String> kv = SParser.wrap(kvs2[i]).getKvs("=");
            assertEquals(i + "th [" + kvs2[i] + "]", "{a=1, b=2, c=4}", kv.toString());
        }
    }

    @Test
    public void testParseOptions() {
        Map<String, String> kvs = new HashMap<>();
        kvs.put("-a", "1");
        kvs.put("-b", "2");
        kvs.put("-c", "3");
        kvs.put("-isX", "true");

        // assertEquals(kvs, StringUtil.parseOptions("-a 1 -b 2 -c 3 -isX"));
        assertTrue(StringUtil.parseKvs("abcd1234*&#$").isEmpty());
    }

    @Test
    public void testGetUnslashedLines() {
        String s = "http://www.sxrb.com/sxxww/\t--fetch-interval=1s --fetch-priority=1010 \\\n" +
                "    --follow-dom=:root --follow-url=.+ --follow-anchor=8,40 \\\n" +
                "    --entity=#content --entity-fields=title:#title,content:#content,publish_time:#publish_time \\\n" +
                "    --collection=#comments --collection-item=.comment --collection-item-fields=publish_time:.comment_publish_time,author:.author,content:.content\n" +
                "http://news.qq.com/\t--fetch-interval=1h --entity=#content\n" +
                "http://news.youth.cn/\t--fetch-interval=1h --entity=#content\\\n" +
                "    --collection=#comments\n" +
                "http://news.163.com/\t--fetch-interval=1h --entity=#content" +
                "\n";

        List<String> lines = StringUtil.getUnslashedLines(s);

        assertEquals(4, lines.size());

        s = "http://sz.sxrb.com/sxxww/dspd/szpd/bwch/\n" +
                "http://sz.sxrb.com/sxxww/dspd/szpd/fcjjjc/\n" +
                "http://sz.sxrb.com/sxxww/dspd/szpd/hydt/";

        lines = StringUtil.getUnslashedLines(s);
        assertEquals(3, lines.size());
    }

    @Test
    public void testLoadSlashedLines() throws IOException {
        String seeds = "@data/lines-with-slashes.txt";

        if (seeds.startsWith("@")) {
            seeds = String.join("\n", ResourceLoader.readAllLines(seeds.substring(1)));
        }

        File seedFile = File.createTempFile("seed", ".txt");
        List<String> unslashedLines = StringUtil.getUnslashedLines(seeds);

//        for (int i = 0; i < unslashedLines.size(); i++) {
//            System.out.println(i + "\t" + unslashedLines.get(i));
//        }

        assertEquals(111, unslashedLines.size());

        FileUtils.writeLines(seedFile, unslashedLines);
        assertEquals(111, Files.readAllLines(seedFile.toPath()).size());

//    System.out.println(StringUtil.getUnslashedLines(seeds).size());
//    System.out.println(seedFile.getAbsolutePath());
    }

    @Test
    public void testGetFirstInteger() {
        String s = "-hello world 999 i love you 520 forever";
        assertEquals(999, StringUtil.getFirstInteger(s, -1));
    }

    @Test
    public void testGetFirstFloatNumber() {
        String s = "-hello world 999.00.0 i love you 520.0 forever";
        assertEquals(999.00f, StringUtil.getFirstFloatNumber(s, Float.MIN_VALUE), 0.1);
    }
}
