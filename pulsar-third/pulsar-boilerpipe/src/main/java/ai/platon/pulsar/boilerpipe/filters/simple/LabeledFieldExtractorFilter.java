package ai.platon.pulsar.boilerpipe.filters.simple;

import ai.platon.pulsar.boilerpipe.utils.ProcessingException;
import com.google.common.collect.ListMultimap;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

/**
 * Marks all blocks that contain a given label as "content".
 */
public final class LabeledFieldExtractorFilter implements TextBlockFilter {

  private final ListMultimap<String, String> labeledRules;
  // private final SortedBidiMap<String, String> labeledRules = new DualTreeBidiMap<>();

  public LabeledFieldExtractorFilter(ListMultimap<String, String> labeledRules) {
    this.labeledRules = labeledRules;
  }

  public boolean process(final TextDocument doc) throws ProcessingException {
    boolean[] changes = {false};

    doc.getTextBlocks().stream().filter(TextBlock::isContent).forEach(tb -> labeledRules.entries().stream()
            .filter(rule -> tb.hasLabel(rule.getValue()))
            .forEach(rule -> {
              doc.setField(rule.getKey(), tb.getText());
              changes[0] = true;
            })
    );
    return changes[0];
  }
}
