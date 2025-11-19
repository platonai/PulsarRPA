package ai.platon.pulsar.common;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RegexExtractorTest {

    @Test
    void testRe1Simple() {
        RegexExtractor extractor = new RegexExtractor();
        String text = "Price: $123";
        String regex = "Price:\\s*\\$(\\d+)";
        assertEquals("123", extractor.re1(text, regex));
    }

    @Test
    void testRe1Group0WholeMatch() {
        RegexExtractor extractor = new RegexExtractor();
        String text = "Price: $123";
        Pattern pattern = Pattern.compile("Price:\\s*\\$(\\d+)");
        assertEquals("Price: $123", extractor.re1(text, pattern, 0));
    }

    @Test
    void testRe1NoMatchReturnsDefault() {
        RegexExtractor extractor = new RegexExtractor().withDefaultValueIfAbsent("N/A");
        String text = "Price: $abc";
        String regex = "Price:\\s*\\$(\\d+)";
        assertEquals("N/A", extractor.re1(text, regex));
    }

    @Test
    void testRe1NullInputs() {
        RegexExtractor extractor = new RegexExtractor();
        Pattern pattern = Pattern.compile("(foo)");
        assertEquals("", extractor.re1(null, pattern, 1));
        assertEquals("", extractor.re1("text", (Pattern) null, 1));
        assertEquals("", extractor.re1("text", "(foo)", -1));
    }

    @Test
    void testRe1StaticDefault() {
        String result = RegexExtractor.re1("abc", "x(\\d+)", 1, "N/A");
        assertEquals("N/A", result);
    }

    @Test
    void testRe2Simple() {
        RegexExtractor extractor = new RegexExtractor();
        String text = "key=value";
        String regex = "(\\w+)=(\\w+)";
        Pair<String, String> p = extractor.re2(text, regex);
        assertEquals("key", p.getLeft());
        assertEquals("value", p.getRight());
    }

    @Test
    void testRe2Group0Allowed() {
        RegexExtractor extractor = new RegexExtractor();
        String text = "k:v";
        Pattern pattern = Pattern.compile("(\\w+):(\\w+)");
        Pair<String, String> p = extractor.re2(text, pattern, 0, 2);
        assertEquals("k:v", p.getLeft());
        assertEquals("v", p.getRight());
    }

    @Test
    void testRe2NoMatchReturnsDefaults() {
        RegexExtractor extractor = new RegexExtractor().withDefaultKeyIfAbsent("K").withDefaultValueIfAbsent("V");
        Pair<String, String> p = extractor.re2("foo", "(a)=(b)");
        assertEquals("K", p.getLeft());
        assertEquals("V", p.getRight());
    }

    @Test
    void testRe2NullInputs() {
        RegexExtractor extractor = new RegexExtractor();
        Pattern pattern = Pattern.compile("(a)=(b)");
        assertEquals(Pair.of("", ""), extractor.re2(null, pattern, 1, 2));
        assertEquals(Pair.of("", ""), extractor.re2("text", (Pattern) null, 1, 2));
        assertEquals(Pair.of("", ""), extractor.re2("text", pattern, -1, 2));
    }
}

