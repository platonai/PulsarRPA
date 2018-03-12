package org.warps.pulsar.boilerpipe.filters.heuristics;

import org.warps.pulsar.boilerpipe.document.BlockLabels;
import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

/**
 * Marks all blocks as content that:
 * <ol>
 * <li>are on the same tag-level as very likely main content (usually the level of the largest
 * block)</li>
 * <li>have a significant number of words, currently: at least 100</li>
 * </ol>
 */
public final class LargeBlockSameTagLevelToContentFilter implements TextBlockFilter {
  public static final LargeBlockSameTagLevelToContentFilter INSTANCE =
      new LargeBlockSameTagLevelToContentFilter();

  private LargeBlockSameTagLevelToContentFilter() {
  }

  public boolean process(final TextDocument doc) throws ProcessingException {

    boolean changes = false;

    int tagLevel = -1;
    for (TextBlock tb : doc.getTextBlocks()) {
      if (tb.isContent() && tb.hasLabel(BlockLabels.VERY_LIKELY_CONTENT)) {
        tagLevel = tb.getTagLevel();
        break;
      }
    }

    if (tagLevel == -1) {
      return false;
    }

    for (TextBlock tb : doc.getTextBlocks()) {
      if (!tb.isContent()) {

        if (tb.getNumWords() >= 100 && tb.getTagLevel() == tagLevel) {
          tb.setIsContent(true);
          changes = true;
        }
      }
    }

    return changes;

  }
}
