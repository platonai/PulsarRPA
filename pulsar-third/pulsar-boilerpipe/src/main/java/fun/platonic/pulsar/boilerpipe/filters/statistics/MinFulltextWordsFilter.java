package fun.platonic.pulsar.boilerpipe.filters.statistics;

import fun.platonic.pulsar.boilerpipe.document.TextBlock;
import fun.platonic.pulsar.boilerpipe.document.TextDocument;
import fun.platonic.pulsar.boilerpipe.filters.TextBlockFilter;
import fun.platonic.pulsar.boilerpipe.filters.heuristics.HeuristicFilterBase;
import fun.platonic.pulsar.boilerpipe.utils.ProcessingException;

/**
 * Keeps only those content blocks which contain at least k full-text words (measured by
 * {@link HeuristicFilterBase#getNumFullTextWords(TextBlock)}). k is 30 by default.
 */
public final class MinFulltextWordsFilter extends HeuristicFilterBase implements TextBlockFilter {
  public static final MinFulltextWordsFilter DEFAULT_INSTANCE = new MinFulltextWordsFilter(30);
  private final int minWords;

  public static MinFulltextWordsFilter getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  public MinFulltextWordsFilter(final int minWords) {
    this.minWords = minWords;
  }

  public boolean process(final TextDocument doc) throws ProcessingException {

    boolean changes = false;

    for (TextBlock tb : doc.getTextBlocks()) {
      if (!tb.isContent()) {
        continue;
      }
      if (getNumFullTextWords(tb) < minWords) {
        tb.setIsContent(false);
        changes = true;
      }

    }

    return changes;

  }
}
