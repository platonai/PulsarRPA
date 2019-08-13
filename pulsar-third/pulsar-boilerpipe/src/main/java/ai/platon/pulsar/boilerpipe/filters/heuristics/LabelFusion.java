package ai.platon.pulsar.boilerpipe.filters.heuristics;

import ai.platon.pulsar.boilerpipe.document.BlockLabels;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Fuses adjacent blocks if their labels are equal.
 */
public final class LabelFusion implements TextBlockFilter {

  public static final LabelFusion INSTANCE = new LabelFusion();

  /**
   * Creates a new {@link LabelFusion} instance.
   */
  private LabelFusion() {
  }

  public boolean process(TextDocument doc) throws ProcessingException {
    List<TextBlock> textBlocks = doc.getTextBlocks();
    if (textBlocks.size() < 2) {
      return false;
    }

    boolean changes = false;
    TextBlock prevBlock = textBlocks.get(0);
    int offset = 1;

    for (Iterator<TextBlock> it = textBlocks.listIterator(offset); it.hasNext(); ) {
      TextBlock block = it.next();

      if (equalLabels(prevBlock.getLabels(), block.getLabels())) {
        prevBlock.mergeNext(block);
        it.remove();
        changes = true;
      } else {
        prevBlock = block;
      }
    }

    return changes;
  }

  private boolean equalLabels(Set<String> labels, Set<String> labels2) {
    if (labels == null || labels2 == null) {
      return false;
    }
    return markupLabelsOnly(labels).equals(markupLabelsOnly(labels2));
  }

  private Set<String> markupLabelsOnly(final Set<String> set1) {
    Set<String> set = new HashSet<String>(set1);
    for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
      final String label = it.next();
      if (!label.startsWith(BlockLabels.MARKUP_PREFIX)) {
        it.remove();
      }
    }
    return set;
  }

}
