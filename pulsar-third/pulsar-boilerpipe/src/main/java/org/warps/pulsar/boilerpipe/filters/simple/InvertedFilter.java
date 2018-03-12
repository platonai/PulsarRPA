package org.warps.pulsar.boilerpipe.filters.simple;

import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

import java.util.List;

/**
 * Reverts the "isContent" flag for all {@link TextBlock}s
 */
public final class InvertedFilter implements TextBlockFilter {
  public static final InvertedFilter INSTANCE = new InvertedFilter();

  private InvertedFilter() {
  }

  public boolean process(TextDocument doc) throws ProcessingException {

    List<TextBlock> tbs = doc.getTextBlocks();
    if (tbs.isEmpty()) {
      return false;
    }
    for (TextBlock tb : tbs) {
      tb.setIsContent(!tb.isContent());
    }

    return true;
  }

}
