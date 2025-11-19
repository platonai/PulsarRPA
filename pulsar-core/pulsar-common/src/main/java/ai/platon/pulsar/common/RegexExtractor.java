package ai.platon.pulsar.common;

import org.apache.commons.lang3.tuple.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting values from text using regular expressions.
 * <p>
 * Key behavior:
 * <ul>
 *   <li>Null inputs or invalid group indexes never throw; methods return configured defaults.</li>
 *   <li>Group {@code 0} (whole match) is allowed; negative indexes return defaults.</li>
 *   <li>Extracted values are trimmed via {@link String#trim()} before returning.</li>
 *   <li>Use {@link #withDefaultKeyIfAbsent(String)} and {@link #withDefaultValueIfAbsent(String)}
 *       to control defaults when nothing matches.</li>
 * </ul>
 * <p>
 * Thread-safety: instances are mutable (defaults can change), so this class is not thread-safe.
 * Create separate instances per thread or avoid mutating defaults after publication.
 */
public class RegexExtractor {

    /** Default key to use when the key capture group is missing, invalid, or unmatched. */
    private String defaultKeyIfAbsent = "";
    /** Default value to use when the value capture group is missing, invalid, or unmatched. */
    private String defaultValueIfAbsent = "";

    /**
     * Create an extractor with empty-string defaults for both key and value when no matches are found.
     */
    public RegexExtractor() {
        // default constructor
    }

    /**
     * Create an extractor with custom defaults to use when match groups are absent.
     *
     * @param defaultKeyIfAbsent Default key to use when the key group is missing or null; null treated as empty string
     * @param defaultValueIfAbsent Default value to use when the value group is missing or null; null treated as empty string
     */
    public RegexExtractor(String defaultKeyIfAbsent, String defaultValueIfAbsent) {
        this.defaultKeyIfAbsent = defaultKeyIfAbsent == null ? "" : defaultKeyIfAbsent;
        this.defaultValueIfAbsent = defaultValueIfAbsent == null ? "" : defaultValueIfAbsent;
    }

    /**
     * Set the default key used when a key group is absent, invalid, or unmatched.
     *
     * @param defaultKeyIfAbsent The default key; null treated as empty string
     * @return This extractor for fluent chaining
     */
    public RegexExtractor withDefaultKeyIfAbsent(String defaultKeyIfAbsent) {
        this.defaultKeyIfAbsent = defaultKeyIfAbsent == null ? "" : defaultKeyIfAbsent;
        return this;
    }

    /**
     * Set the default value used when a value group is absent, invalid, or unmatched.
     *
     * @param defaultValueIfAbsent The default value; null treated as empty string
     * @return This extractor for fluent chaining
     */
    public RegexExtractor withDefaultValueIfAbsent(String defaultValueIfAbsent) {
        this.defaultValueIfAbsent = defaultValueIfAbsent == null ? "" : defaultValueIfAbsent;
        return this;
    }

    // -------------------------------- re1 --------------------------------

    /**
     * Extract a single captured group using a regex provided as a string.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Compiles the regex and returns group {@code 1} by default.</li>
     *   <li>On null regex or no match, returns the configured default value.</li>
     * </ul>
     *
     * @param text The input text to match; may be null
     * @param regex The regex string; if null, returns the default value
     * @return The trimmed captured value or the configured default when unmatched
     */
    public String re1(String text, String regex) {
        if (regex == null) return defaultValueIfAbsent;
        return re1(text, Pattern.compile(regex), 1);
    }

    /**
     * Extract a single captured group using a precompiled {@link Pattern}.
     * Returns group {@code 1} when a match is found.
     *
     * @param text The input text to match; may be null
     * @param pattern The compiled regex; may be null
     * @return The trimmed captured value or the configured default when unmatched
     */
    public String re1(String text, Pattern pattern) {
        return re1(text, pattern, 1);
    }

    /**
     * Extract a specific captured group using a regex provided as a string.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Compiles the regex and returns the specified {@code valueGroup} when matched.</li>
     *   <li>Group {@code 0} (whole match) is allowed; negative groups return the default.</li>
     *   <li>On null regex or no match, returns the configured default value.</li>
     * </ul>
     *
     * @param text The input text to match; may be null
     * @param regex The regex string; if null, returns the default value
     * @param valueGroup The group index to return; 0 for whole match
     * @return The trimmed captured value or the configured default when unmatched
     */
    public String re1(String text, String regex, int valueGroup) {
        if (regex == null) return defaultValueIfAbsent;
        return re1(text, Pattern.compile(regex), valueGroup);
    }

    /**
     * Extract a specific captured group using a precompiled {@link Pattern}.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Returns the specified {@code valueGroup} when matched.</li>
     *   <li>Group {@code 0} (whole match) is allowed; negative groups return the default.</li>
     *   <li>Never throws on invalid indexes; returns the configured default value instead.</li>
     * </ul>
     *
     * @param text The input text to match; may be null
     * @param pattern The compiled regex; may be null
     * @param valueGroup The group index to return; 0 for whole match
     * @return The trimmed captured value or the configured default when unmatched
     */
    public String re1(String text, Pattern pattern, int valueGroup) {
        String result = defaultValueIfAbsent;
        if (text == null || pattern == null || valueGroup < 0) {
            return result;
        }

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            int groupCount = matcher.groupCount();
            // group(0) is allowed, so check valueGroup == 0 or <= groupCount
            if (valueGroup == 0 || valueGroup <= groupCount) {
                String v;
                try {
                    v = matcher.group(valueGroup);
                } catch (IndexOutOfBoundsException ex) {
                    v = null;
                }

                if (v != null) {
                    result = v.trim();
                }
            }
        }

        return result;
    }

    // Static convenience methods (non-breaking additions)

    /**
     * Extract a single value using a precompiled pattern and an explicit default to return when unmatched.
     *
     * @param text The input text to match; may be null
     * @param pattern The compiled regex; may be null
     * @param valueGroup The group index to return; 0 for whole match
     * @param defaultValue The default value to return when unmatched or invalid
     * @return The trimmed captured value or {@code defaultValue} when unmatched
     */
    public static String re1(String text, Pattern pattern, int valueGroup, String defaultValue) {
        RegexExtractor extractor = new RegexExtractor().withDefaultValueIfAbsent(defaultValue);
        return extractor.re1(text, pattern, valueGroup);
    }

    /**
     * Extract a single value using a regex string and an explicit default to return when unmatched.
     *
     * @param text The input text to match; may be null
     * @param regex The regex string; if null, returns {@code defaultValue}
     * @param valueGroup The group index to return; 0 for whole match
     * @param defaultValue The default value to return when unmatched or invalid
     * @return The trimmed captured value or {@code defaultValue} when unmatched
     */
    public static String re1(String text, String regex, int valueGroup, String defaultValue) {
        if (regex == null) return defaultValue;
        return re1(text, Pattern.compile(regex), valueGroup, defaultValue);
    }

    // -------------------------------- re2 --------------------------------

    /**
     * Extract a key-value pair using a regex provided as a string.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Compiles the regex and returns groups {@code 1} (key) and {@code 2} (value).</li>
     *   <li>On null regex or no match, returns a pair of configured defaults.</li>
     * </ul>
     *
     * @param text The input text to match; may be null
     * @param regex The regex string; if null, returns defaults
     * @return A pair of trimmed key and value, or configured defaults when unmatched
     */
    public Pair<String, String> re2(String text, String regex) {
        if (regex == null) return Pair.of(defaultKeyIfAbsent, defaultValueIfAbsent);
        return re2(text, Pattern.compile(regex), 1, 2);
    }

    /**
     * Extract a key-value pair using a precompiled {@link Pattern}.
     * Returns groups {@code 1} (key) and {@code 2} (value) when matched.
     *
     * @param text The input text to match; may be null
     * @param pattern The compiled regex; may be null
     * @return A pair of trimmed key and value, or configured defaults when unmatched
     */
    public Pair<String, String> re2(String text, Pattern pattern) {
        return re2(text, pattern, 1, 2);
    }

    /**
     * Extract a key-value pair using a regex provided as a string with explicit group indexes.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Compiles the regex and returns the specified {@code keyGroup} and {@code valueGroup} when matched.</li>
     *   <li>Group {@code 0} (whole match) is allowed for either key or value; negative groups return defaults.</li>
     *   <li>On null regex or no match, returns a pair of configured defaults.</li>
     * </ul>
     *
     * @param text The input text to match; may be null
     * @param regex The regex string; if null, returns defaults
     * @param keyGroup The group index to use for the key; 0 for whole match
     * @param valueGroup The group index to use for the value; 0 for whole match
     * @return A pair of trimmed key and value, or configured defaults when unmatched
     */
    public Pair<String, String> re2(String text, String regex, int keyGroup, int valueGroup) {
        if (regex == null) return Pair.of(defaultKeyIfAbsent, defaultValueIfAbsent);
        return re2(text, Pattern.compile(regex), keyGroup, valueGroup);
    }

    /**
     * Extract a key-value pair using a precompiled {@link Pattern} with explicit group indexes.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Returns the specified {@code keyGroup} and {@code valueGroup} when matched.</li>
     *   <li>Group {@code 0} (whole match) is allowed for either key or value; negative groups return defaults.</li>
     *   <li>Never throws on invalid indexes; returns configured defaults instead.</li>
     * </ul>
     *
     * @param text The input text to match; may be null
     * @param pattern The compiled regex; may be null
     * @param keyGroup The group index to use for the key; 0 for whole match
     * @param valueGroup The group index to use for the value; 0 for whole match
     * @return A pair of trimmed key and value, or configured defaults when unmatched
     */
    public Pair<String, String> re2(String text, Pattern pattern, int keyGroup, int valueGroup) {
        Pair<String, String> parts = Pair.of(defaultKeyIfAbsent, defaultValueIfAbsent);
        if (text == null || pattern == null || keyGroup < 0 || valueGroup < 0) {
            return parts;
        }

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            int groupCount = matcher.groupCount();
            boolean keyOk = (keyGroup == 0 || keyGroup <= groupCount);
            boolean valOk = (valueGroup == 0 || valueGroup <= groupCount);
            if (keyOk && valOk) {
                String k;
                String v;
                try {
                    k = matcher.group(keyGroup);
                    v = matcher.group(valueGroup);
                } catch (IndexOutOfBoundsException ex) {
                    k = null;
                    v = null;
                }

                if (k != null && v != null) {
                    parts = Pair.of(k.trim(), v.trim());
                }
            }
        }

        return parts;
    }
}
