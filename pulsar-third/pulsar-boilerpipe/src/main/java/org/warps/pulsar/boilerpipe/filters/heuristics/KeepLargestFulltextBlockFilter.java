package org.warps.pulsar.boilerpipe.filters.heuristics;

import org.warps.pulsar.boilerpipe.document.BlockLabels;
import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

import java.util.List;

/**
 * Keeps the largest {@link TextBlock} only (by the number of words). In case of more than one block
 * with the same number of words, the first block is chosen. All discarded blocks are marked
 * "not content" and flagged as {@link BlockLabels#MIGHT_BE_CONTENT}.
 *
 * As opposed to {@link KeepLargestBlockFilter}, the number of words are computed using
 * {@link HeuristicFilterBase#getNumFullTextWords(TextBlock)}, which only counts words that occur in
 * text elements with at least 9 words and are thus believed to be full text.
 *
 * NOTE: Without language-specific fine-tuning (i.e., running the default instance), this filter may
 * lead to suboptimal results. You better use {@link KeepLargestBlockFilter} instead, which works at
 * the level of number-of-words instead of text densities.
 */
public final class KeepLargestFulltextBlockFilter extends HeuristicFilterBase implements
    TextBlockFilter {
  public static final KeepLargestFulltextBlockFilter INSTANCE =
      new KeepLargestFulltextBlockFilter();

  public boolean process(final TextDocument doc) throws ProcessingException {
    List<TextBlock> textBlocks = doc.getTextBlocks();
    if (textBlocks.size() < 2) {
      return false;
    }

    int max = -1;
    TextBlock largestBlock = null;
    for (TextBlock tb : textBlocks) {
      if (!tb.isContent()) {
        continue;
      }
      int numWords = getNumFullTextWords(tb);
      if (numWords > max) {
        largestBlock = tb;
        max = numWords;
      }
    }

    if (largestBlock == null) {
      return false;
    }

    for (TextBlock tb : textBlocks) {
      if (tb == largestBlock) {
        tb.setIsContent(true);
      } else {
        tb.setIsContent(false);
        tb.addLabel(BlockLabels.MIGHT_BE_CONTENT);
      }
    }

    return true;
  }
}
