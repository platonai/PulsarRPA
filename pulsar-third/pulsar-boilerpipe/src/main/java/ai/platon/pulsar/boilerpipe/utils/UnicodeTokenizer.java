package ai.platon.pulsar.boilerpipe.utils;

import java.util.regex.Pattern;

/**
 * Tokenizes text according to Unicode word boundaries and strips off non-word characters.
 *
 * TODO : employ a better tokenize utility
 */
public class UnicodeTokenizer {
  private static final Pattern PAT_WORD_BOUNDARY = Pattern.compile("\\b");
  private static final Pattern PAT_NOT_WORD_BOUNDARY = Pattern.compile("[\u2063]*([\\\"'\\.,\\!\\@\\-\\:\\;\\$\\?\\(\\)/])[\u2063]*");

  /**
   * Tokenizes the text and returns an array of tokens.
   *
   * @param text The text
   * @return The tokens
   */
  public static String[] tokenize(final CharSequence text) {
    return PAT_NOT_WORD_BOUNDARY.matcher(PAT_WORD_BOUNDARY.matcher(text).replaceAll("\u2063"))
        .replaceAll("$1").replaceAll("[ \u2063]+", " ").trim().split("[ ]+");
  }
}
