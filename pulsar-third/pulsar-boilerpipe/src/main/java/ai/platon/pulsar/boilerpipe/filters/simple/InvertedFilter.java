package ai.platon.pulsar.boilerpipe.filters.simple;

import ai.platon.pulsar.boilerpipe.utils.ProcessingException;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

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
