package org.warps.pulsar.boilerpipe.filters.heuristics;

import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Adds the labels of the preceding block to the current block, optionally adding a prefix.
 */
public final class AddPrecedingLabelsFilter implements TextBlockFilter {

  public static final AddPrecedingLabelsFilter INSTANCE = new AddPrecedingLabelsFilter("");
  public static final AddPrecedingLabelsFilter INSTANCE_PRE = new AddPrecedingLabelsFilter("^");

  private final String labelPrefix;

  /**
   * Creates a new {@link AddPrecedingLabelsFilter} instance.
   *
   * @param labelPrefix The maximum distance in blocks.
   */
  public AddPrecedingLabelsFilter(final String labelPrefix) {
    this.labelPrefix = labelPrefix;
  }

  public boolean process(TextDocument doc) throws ProcessingException {
    List<TextBlock> textBlocks = doc.getTextBlocks();
    if (textBlocks.size() < 2) {
      return false;
    }

    boolean changes = false;
    int remaining = textBlocks.size();

    TextBlock blockBelow = null;
    TextBlock block;
    for (ListIterator<TextBlock> it = textBlocks.listIterator(textBlocks.size()); it.hasPrevious(); ) {
      if (--remaining <= 0) {
        break;
      }
      if (blockBelow == null) {
        blockBelow = it.previous();
        continue;
      }
      block = it.previous();

      Set<String> labels = block.getLabels();
      if (labels != null && !labels.isEmpty()) {
        for (String l : labels) {
          blockBelow.addLabel(labelPrefix + l);
        }
        changes = true;
      }
      blockBelow = block;
    }

    return changes;
  }
}
