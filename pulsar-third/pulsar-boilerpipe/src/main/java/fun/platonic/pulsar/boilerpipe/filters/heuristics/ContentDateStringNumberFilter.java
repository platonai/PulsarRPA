package fun.platonic.pulsar.boilerpipe.filters.heuristics;

import fun.platonic.pulsar.boilerpipe.document.BlockLabels;
import fun.platonic.pulsar.boilerpipe.document.TextBlock;
import fun.platonic.pulsar.boilerpipe.document.TextDocument;
import fun.platonic.pulsar.boilerpipe.filters.TextBlockFilter;
import fun.platonic.pulsar.boilerpipe.utils.ProcessingException;

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

  public boolean process(TextDocument doc) throws ProcessingException {
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
