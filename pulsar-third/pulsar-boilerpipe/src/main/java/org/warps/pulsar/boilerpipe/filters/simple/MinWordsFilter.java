package org.warps.pulsar.boilerpipe.filters.simple;

import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

/**
 * Keeps only those content blocks which contain at least <em>k</em> words.
 */
public final class MinWordsFilter implements TextBlockFilter {
  private final int minWords;

  public MinWordsFilter(final int minWords) {
    this.minWords = minWords;
  }

  public boolean process(final TextDocument doc) throws ProcessingException {

    boolean changes = false;

    for (TextBlock tb : doc.getTextBlocks()) {
      if (!tb.isContent()) {
        continue;
      }
      if (tb.getNumWords() < minWords) {
        tb.setIsContent(false);
        changes = true;
      }

    }

    return changes;

  }
}
