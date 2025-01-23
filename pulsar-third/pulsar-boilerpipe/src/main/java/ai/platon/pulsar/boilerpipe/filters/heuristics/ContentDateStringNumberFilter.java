package ai.platon.pulsar.boilerpipe.filters.heuristics;

import ai.platon.pulsar.boilerpipe.document.BlockLabels;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.BoiTextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

/**
 * Classifies {@link TextBlock}s as content/not-content through rules that have been determined
 * using the C4.8 machine learning algorithm, as described in the paper
 * "Boilerplate Detection using Shallow Text Features" (WSDM 2010), particularly using number of
 * words per block and link density per block.
 */
public class ContentDateStringNumberFilter implements TextBlockFilter {

  public static final ContentDateStringNumberFilter INSTANCE = new ContentDateStringNumberFilter();

  /**
   * Returns the singleton instance for ContentDateStringNumberFilter.
   */
  public static ContentDateStringNumberFilter getInstance() {
    return INSTANCE;
  }

  public boolean process(BoiTextDocument doc) throws ProcessingException {
    boolean hasChanges = false;

    if (doc.getDateTimeCount() >= 8 && !doc.getPageCategory().isDetail()) {
      for (TextBlock tb : doc.getTextBlocks()) {
        tb.addLabel(BlockLabels.TOO_MANY_DATE_STRING_CONTENT);
        tb.setIsContent(false);
        hasChanges = true;
      }
    }

    return hasChanges;
  }
}
