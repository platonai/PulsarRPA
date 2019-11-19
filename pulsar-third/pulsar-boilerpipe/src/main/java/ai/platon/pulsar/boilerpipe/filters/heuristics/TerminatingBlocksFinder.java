package ai.platon.pulsar.boilerpipe.filters.heuristics;

import ai.platon.pulsar.boilerpipe.document.BlockLabels;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;

/**
 * Finds blocks which are potentially indicating the end of an article text and marks them with
 * {@link BlockLabels#INDICATES_END_OF_TEXT}. This can be used in conjunction with a downstream
 * {@link IgnoreBlocksAfterContentFilter}.
 *
 * @see IgnoreBlocksAfterContentFilter
 */
public class TerminatingBlocksFinder implements TextBlockFilter {
  public static final TerminatingBlocksFinder INSTANCE = new TerminatingBlocksFinder();

  private final Set<String> contains = Sets.newTreeSet();
  private final Set<String> startsWith = Sets.newTreeSet();

  /**
   * Returns the singleton instance for TerminatingBlocksFinder.
   */
  public static TerminatingBlocksFinder getInstance() {
    return INSTANCE;
  }

  public TerminatingBlocksFinder() {

  }

  public TerminatingBlocksFinder(Collection<String> contains, Collection<String> startsWith) {
    this.contains.addAll(contains);
    this.startsWith.addAll(startsWith);
  }

  // public static long timeSpent = 0;

  public boolean process(TextDocument doc) throws ProcessingException {
    boolean changes = false;

    // long t = System.currentTimeMillis();

    for (TextBlock tb : doc.getTextBlocks()) {
      final int numWords = tb.getNumWords();
      if (numWords < 15) {
        final String text = tb.getText().trim();
        final int len = text.length();
        if (len >= 8) {
          final String textLC = text.toLowerCase();

          boolean isTerminatingBlock = startsWithNumber(textLC, len, " comments", " users responded in");

          if (!isTerminatingBlock) {
            for (String str : contains) {
              if (textLC.contains(str.toLowerCase())) {
                isTerminatingBlock = true;
                break;
              }
            } // for
          }

          if (!isTerminatingBlock) {
            for (String str : startsWith) {
              if (textLC.startsWith(str.toLowerCase())) {
                isTerminatingBlock = true;
                break;
              }
            } // for
          }

          if (isTerminatingBlock) {
            tb.addLabel(BlockLabels.INDICATES_END_OF_TEXT);
            changes = true;
          }
        } else if (tb.getLinkDensity() == 1.0) {
          if (text.equals("Comment")) {
            tb.addLabel(BlockLabels.INDICATES_END_OF_TEXT);
          }
        }
      }
    }

    // timeSpent += System.currentTimeMillis() - t;

    return changes;
  }

  /**
   * Checks whether the given text t starts with a sequence of digits, followed by one of the given
   * strings.
   *
   * @param t The text to examine
   * @param len The length of the text to examine
   * @param str Any strings that may follow the digits.
   * @return true if at least one combination matches
   */
  private static boolean startsWithNumber(final String t, final int len, final String... str) {
    int j = 0;
    while (j < len && isDigit(t.charAt(j))) {
      j++;
    }
    if (j != 0) {
      for (String s : str) {
        if (t.startsWith(s, j)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isDigit(final char c) {
    return c >= '0' && c <= '9';
  }

}
