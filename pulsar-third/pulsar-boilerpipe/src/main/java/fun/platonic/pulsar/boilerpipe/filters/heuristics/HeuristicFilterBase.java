package fun.platonic.pulsar.boilerpipe.filters.heuristics;

import fun.platonic.pulsar.boilerpipe.document.TextBlock;

/**
 * Base class for some heuristics that are used by boilerpipe filters.
 */
public abstract class HeuristicFilterBase {

  protected static int getNumFullTextWords(final TextBlock tb) {
    return getNumFullTextWords(tb, 9);
  }

  protected static int getNumFullTextWords(final TextBlock tb, float minTextDensity) {
    if (tb.getTextDensity() >= minTextDensity) {
      return tb.getNumWords();
    } else {
      return 0;
    }
  }
}
