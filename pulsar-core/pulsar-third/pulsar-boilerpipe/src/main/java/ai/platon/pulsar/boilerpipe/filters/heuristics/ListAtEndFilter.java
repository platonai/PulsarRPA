package ai.platon.pulsar.boilerpipe.filters.heuristics;

import ai.platon.pulsar.boilerpipe.document.BlockLabels;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.BoiTextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

/**
 * Marks nested list-item blocks after the end of the main content.
 */
public final class ListAtEndFilter implements TextBlockFilter {
  public static final ListAtEndFilter INSTANCE = new ListAtEndFilter();

  private ListAtEndFilter() {
  }

  public boolean process(final BoiTextDocument doc) throws ProcessingException {

    boolean changes = false;

    int tagLevel = Integer.MAX_VALUE;
    for (TextBlock tb : doc.getTextBlocks()) {
      if (tb.isContent() && tb.hasLabel(BlockLabels.VERY_LIKELY_CONTENT)) {
        tagLevel = tb.getTagLevel();
      } else {
        if (tb.getTagLevel() > tagLevel && tb.hasLabel(BlockLabels.MIGHT_BE_CONTENT)
            && tb.hasLabel(BlockLabels.LI) && tb.getLinkDensity() == 0) {
          tb.setIsContent(true);
          changes = true;
        } else {
          tagLevel = Integer.MAX_VALUE;
        }
      }
    }

    return changes;

  }
}
