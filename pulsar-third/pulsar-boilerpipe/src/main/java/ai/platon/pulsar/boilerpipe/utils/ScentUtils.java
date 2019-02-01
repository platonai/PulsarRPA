package ai.platon.pulsar.boilerpipe.utils;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ai.platon.pulsar.boilerpipe.utils.Scent.BAD_PHRASE_IN_NAME;

/**
 * Created by vincent on 16-10-27.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class ScentUtils {

  public static boolean checkFieldIsAPersonName(String fieldName) {
    return fieldName.equals("author") || fieldName.equals("director");
  }

  public static Map<String, String> extract(String text, ListMultimap<String, String> regexFieldRules) {
    return extract(text, regexFieldRules, 1, 2);
  }

  public static Map<String, String> extract(String text, ListMultimap<String, String> regexFieldRules, int keyGroup, int valueGroup) {
    Map<String, String> results = new LinkedHashMap<>();

    for (Map.Entry<String, String> rule : regexFieldRules.entries()) {
      String[] parts = extractToArray(text, Pattern.compile(rule.getValue()), keyGroup, valueGroup);
      if (parts[0].length() > 0 && parts[1].length() > 0) {
        String key = rule.getKey();
        String value = filterExtractedValue(key, parts[1]);

        if (value == null || value.isEmpty()) {
          continue;
        }

        // The value is a name, but the value does not like a name
        if (checkFieldIsAPersonName(key)) {
          if (Arrays.stream(Scent.BAD_PHRASE_IN_NAME).anyMatch(value::contains)) {
            value = null;
          }
        }

        if (value == null || value.isEmpty()) {
          continue;
        }

        Integer maxLength = Scent.MAX_FIELD_LENGTH_MAP.get(key);
        if (maxLength == null) {
          results.put(key, value);
        } else if (value.length() <= maxLength) {
          results.put(key, value);
        }
      }
    } // for

    return results;
  }

  private static String filterExtractedValue(String key, String value) {
    for (String bounder : Scent.REGEX_FIELD_BOUNDERS) {
      if (value.endsWith(bounder)) {
        value = StringUtils.substringBefore(value, bounder);
        break;
      }
    }

    return value;
  }

  public static String[] extractToArray(String text, Pattern pattern) {
    return extractToArray(text, pattern, 1, 2);
  }

  public static String[] extractToArray(String text, Pattern pattern, int keyGroup, int valueGroup) {
    String[] parts = {"", ""};

    Matcher matcher = pattern.matcher(text);

    if (matcher.find()) {
      int groupCount = matcher.groupCount();
      if (keyGroup <= groupCount && valueGroup <= groupCount) {
        String k = matcher.group(keyGroup);
        String v = matcher.group(valueGroup);

        if (k != null && v != null) {
          parts[0] = k.trim();
          parts[1] = v.trim();
        }
      }
    }

    return parts;
  }
}
