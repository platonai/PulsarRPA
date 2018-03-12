package org.warps.pulsar.boilerpipe.filters.heuristics;

import org.warps.pulsar.boilerpipe.document.BlockLabels;
import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

import java.util.List;
import java.util.ListIterator;

/**
 * Marks trailing headlines ({@link TextBlock}s that have the label {@link BlockLabels#HEADING})
 * as boilerplate. Trailing means they are marked content and are below any other content block.
 */
public final class TrailingHeadlineToBoilerplateFilter implements TextBlockFilter {
  public static final TrailingHeadlineToBoilerplateFilter INSTANCE =
      new TrailingHeadlineToBoilerplateFilter();

  /**
   * Returns the singleton instance for ExpandTitleToContentFilter.
   */
  public static TrailingHeadlineToBoilerplateFilter getInstance() {
    return INSTANCE;
  }

  public boolean process(TextDocument doc) throws ProcessingException {
    boolean changes = false;

    List<TextBlock> list = doc.getTextBlocks();

    for (ListIterator<TextBlock> it = list.listIterator(list.size()); it.hasPrevious(); ) {
      TextBlock tb = it.previous();
      if (tb.isContent()) {
        if (tb.hasLabel(BlockLabels.HEADING)) {
          tb.setIsContent(false);
          changes = true;
        } else {
          break;
        }
      }
    }

    return changes;
  }

}
