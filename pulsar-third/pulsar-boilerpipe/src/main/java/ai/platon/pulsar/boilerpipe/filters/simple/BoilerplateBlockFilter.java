package ai.platon.pulsar.boilerpipe.filters.simple;

import ai.platon.pulsar.boilerpipe.document.BlockLabels;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.BoiTextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

import java.util.Iterator;
import java.util.List;

/**
 * Removes {@link TextBlock}s which have explicitly been marked as "not content".
 */
public final class BoilerplateBlockFilter implements TextBlockFilter {
  public static final BoilerplateBlockFilter INSTANCE = new BoilerplateBlockFilter(null);
  public static final BoilerplateBlockFilter INSTANCE_KEEP_TITLE = new BoilerplateBlockFilter(BlockLabels.CONTENT_TITLE);
  private final String labelToKeep;

  /**
   * Returns the singleton instance for BoilerplateBlockFilter.
   */
  public static BoilerplateBlockFilter getInstance() {
    return INSTANCE;
  }

  public BoilerplateBlockFilter(final String labelToKeep) {
    this.labelToKeep = labelToKeep;
  }

  public boolean process(BoiTextDocument doc) throws ProcessingException {
    List<TextBlock> textBlocks = doc.getTextBlocks();
    boolean hasChanges = false;

    for (Iterator<TextBlock> it = textBlocks.iterator(); it.hasNext(); ) {
      TextBlock tb = it.next();
      if (!tb.isContent() && (labelToKeep == null || !tb.hasLabel(BlockLabels.CONTENT_TITLE) || !tb.hasLabel(BlockLabels.CONTENT_TITLE))) {
        it.remove();
        hasChanges = true;
      }
    }

    return hasChanges;
  }
}
