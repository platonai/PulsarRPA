package org.warps.pulsar.boilerpipe.filters.heuristics;

import org.warps.pulsar.boilerpipe.document.BlockLabels;
import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

import java.util.List;
import java.util.ListIterator;

/**
 * Keeps the largest {@link TextBlock} only (by the number of words). In case of more than one block
 * with the same number of words, the first block is chosen. All discarded blocks are marked
 * "not content" and flagged as {@link BlockLabels#MIGHT_BE_CONTENT}.
 *
 * Note that, by default, only TextBlocks marked as "content" are taken into consideration.
 */
public final class KeepLargestBlockFilter implements TextBlockFilter {
  public static final KeepLargestBlockFilter INSTANCE = new KeepLargestBlockFilter(false, 0);
  public static final KeepLargestBlockFilter INSTANCE_EXPAND_TO_SAME_TAGLEVEL =
      new KeepLargestBlockFilter(true, 0);
  public static final KeepLargestBlockFilter INSTANCE_EXPAND_TO_SAME_TAGLEVEL_MIN_WORDS =
      new KeepLargestBlockFilter(true, 150);
  private final boolean expandToSameLevelText;
  private final int minWords;

  public KeepLargestBlockFilter(boolean expandToSameLevelText, final int minWords) {
    this.expandToSameLevelText = expandToSameLevelText;
    this.minWords = minWords;
  }

  public boolean process(final TextDocument doc) throws ProcessingException {
    List<TextBlock> textBlocks = doc.getTextBlocks();
    if (textBlocks.size() < 2) {
      return false;
    }

    int maxNumWords = -1;
    TextBlock largestBlock = null;

    int level = -1;

    int i = 0;
    int n = -1;
    for (TextBlock tb : textBlocks) {
      if (tb.isContent()) {
        final int nw = tb.getNumWords();

        if (nw > maxNumWords) {
          largestBlock = tb;
          maxNumWords = nw;

          n = i;

          if (expandToSameLevelText) {
            level = tb.getTagLevel();
          }
        }
      }
      i++;
    }
    for (TextBlock tb : textBlocks) {
      if (tb == largestBlock) {
        tb.setIsContent(true);
        tb.addLabel(BlockLabels.VERY_LIKELY_CONTENT);
      } else {
        tb.setIsContent(false);
        tb.addLabel(BlockLabels.MIGHT_BE_CONTENT);
      }
    }
    if (expandToSameLevelText && n != -1) {

      for (ListIterator<TextBlock> it = textBlocks.listIterator(n); it.hasPrevious(); ) {
        TextBlock tb = it.previous();
        final int tl = tb.getTagLevel();
        if (tl < level) {
          break;
        } else if (tl == level) {
          if (tb.getNumWords() >= minWords) {
            tb.setIsContent(true);
          }
        }
      }
      for (ListIterator<TextBlock> it = textBlocks.listIterator(n); it.hasNext(); ) {
        TextBlock tb = it.next();
        final int tl = tb.getTagLevel();
        if (tl < level) {
          break;
        } else if (tl == level) {
          if (tb.getNumWords() >= minWords) {
            tb.setIsContent(true);
          }
        }
      }
    }

    return true;
  }
}
