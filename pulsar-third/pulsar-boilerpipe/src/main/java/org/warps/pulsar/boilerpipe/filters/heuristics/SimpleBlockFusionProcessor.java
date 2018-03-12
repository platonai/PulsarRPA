package org.warps.pulsar.boilerpipe.filters.heuristics;

import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

import java.util.Iterator;
import java.util.List;

/**
 * Merges two subsequent blocks if their text densities are equal.
 */
public class SimpleBlockFusionProcessor implements TextBlockFilter {
  public static final SimpleBlockFusionProcessor INSTANCE = new SimpleBlockFusionProcessor();

  /**
   * Returns the singleton instance for BlockFusionProcessor.
   */
  public static SimpleBlockFusionProcessor getInstance() {
    return INSTANCE;
  }

  public boolean process(TextDocument doc) throws ProcessingException {
    List<TextBlock> textBlocks = doc.getTextBlocks();
    boolean changes = false;

    if (textBlocks.size() < 2) {
      return false;
    }

    TextBlock b1 = textBlocks.get(0);
    for (Iterator<TextBlock> it = textBlocks.listIterator(1); it.hasNext(); ) {
      TextBlock b2 = it.next();

      final boolean similar = (b1.getTextDensity() == b2.getTextDensity());

      if (similar) {
        b1.mergeNext(b2);
        it.remove();
        changes = true;
      } else {
        b1 = b2;
      }
    }

    return changes;
  }
}
