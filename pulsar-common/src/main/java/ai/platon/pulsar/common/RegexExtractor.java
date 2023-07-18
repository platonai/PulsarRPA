package ai.platon.pulsar.common;

import org.apache.commons.lang3.tuple.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vincent on 17-8-3.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public class RegexExtractor {

    private String defaultKeyIfAbsent = "";
    private String defaultValueIfAbsent = "";

    /**
     * <p>Constructor for RegexExtractor.</p>
     */
    public RegexExtractor() {

    }

    /**
     * <p>re1.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param regex a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String re1(String text, String regex) {
        return re1(text, Pattern.compile(regex), 1);
    }

    /**
     * <p>re1.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param pattern a {@link java.util.regex.Pattern} object.
     * @return a {@link java.lang.String} object.
     */
    public String re1(String text, Pattern pattern) {
        return re1(text, pattern, 1);
    }

    /**
     * <p>re1.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param regex a {@link java.lang.String} object.
     * @param valueGroup a int.
     * @return a {@link java.lang.String} object.
     */
    public String re1(String text, String regex, int valueGroup) {
        return re1(text, Pattern.compile(regex));
    }

    /**
     * <p>re1.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param pattern a {@link java.util.regex.Pattern} object.
     * @param valueGroup a int.
     * @return a {@link java.lang.String} object.
     */
    public String re1(String text, Pattern pattern, int valueGroup) {
        String result = defaultValueIfAbsent;

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            int groupCount = matcher.groupCount();
            if (valueGroup <= groupCount) {
                String v = matcher.group(valueGroup);

                if (v != null) {
                    result = v.trim();
                }
            }
        }

        return result;
    }

    /**
     * <p>re2.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param regex a {@link java.lang.String} object.
     * @return a {@link org.apache.commons.lang3.tuple.Pair} object.
     */
    public Pair<String, String> re2(String text, String regex) {
        return re2(text, Pattern.compile(regex), 1, 2);
    }

    /**
     * <p>re2.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param pattern a {@link java.util.regex.Pattern} object.
     * @return a {@link org.apache.commons.lang3.tuple.Pair} object.
     */
    public Pair<String, String> re2(String text, Pattern pattern) {
        return re2(text, pattern, 1, 2);
    }

    /**
     * <p>re2.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param regex a {@link java.lang.String} object.
     * @param keyGroup a int.
     * @param valueGroup a int.
     * @return a {@link org.apache.commons.lang3.tuple.Pair} object.
     */
    public Pair<String, String> re2(String text, String regex, int keyGroup, int valueGroup) {
        return re2(text, Pattern.compile(regex), keyGroup, valueGroup);
    }

    /**
     * <p>re2.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param pattern a {@link java.util.regex.Pattern} object.
     * @param keyGroup a int.
     * @param valueGroup a int.
     * @return a {@link org.apache.commons.lang3.tuple.Pair} object.
     */
    public Pair<String, String> re2(String text, Pattern pattern, int keyGroup, int valueGroup) {
        Pair<String, String> parts = Pair.of(defaultKeyIfAbsent, defaultValueIfAbsent);

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            int groupCount = matcher.groupCount();
            if (keyGroup <= groupCount && valueGroup <= groupCount) {
                String k = matcher.group(keyGroup);
                String v = matcher.group(valueGroup);

                if (k != null && v != null) {
                    parts = Pair.of(k.trim(), v.trim());
                }
            }
        }

        return parts;
    }
}
