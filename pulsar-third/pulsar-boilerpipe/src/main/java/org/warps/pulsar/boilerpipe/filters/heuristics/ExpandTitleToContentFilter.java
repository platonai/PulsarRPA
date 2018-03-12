package org.warps.pulsar.boilerpipe.filters.heuristics;

import org.warps.pulsar.boilerpipe.document.BlockLabels;
import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

/**
 * Marks all {@link TextBlock}s "content" which are between the headline and the part that has
 * already been marked content, if they are marked {@link BlockLabels#MIGHT_BE_CONTENT}.
 *
 * This filter is quite specific to the news domain.
 */
public final class ExpandTitleToContentFilter implements TextBlockFilter {
  public static final ExpandTitleToContentFilter INSTANCE = new ExpandTitleToContentFilter();

  /**
   * Returns the singleton instance for ExpandTitleToContentFilter.
   */
  public static ExpandTitleToContentFilter getInstance() {
    return INSTANCE;
  }

  public boolean process(TextDocument doc) throws ProcessingException {
    int i = 0;
    int title = -1;
    int contentStart = -1;
    for (TextBlock tb : doc.getTextBlocks()) {
      if (contentStart == -1 && tb.hasLabel(BlockLabels.CONTENT_TITLE)) {
        title = i;
        contentStart = -1;
      }
      if (contentStart == -1 && tb.isContent()) {
        contentStart = i;
      }

      i++;
    }

    if (contentStart <= title || title == -1) {
      return false;
    }

    boolean changes = false;
    for (TextBlock tb : doc.getTextBlocks().subList(title, contentStart)) {
      if (tb.hasLabel(BlockLabels.MIGHT_BE_CONTENT)) {
        changes = tb.setIsContent(true) | changes;
      }
    }
    return changes;
  }

}
