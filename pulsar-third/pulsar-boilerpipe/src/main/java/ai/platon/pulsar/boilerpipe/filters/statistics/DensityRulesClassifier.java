package ai.platon.pulsar.boilerpipe.filters.statistics;

import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

import java.util.List;
import java.util.ListIterator;

/**
 * Classifies {@link TextBlock}s as content/not-content through rules that have been determined
 * using the C4.8 machine learning algorithm, as described in the paper
 * "Boilerplate Detection using Shallow Text Features", particularly using text densities and link
 * densities.
 */
public class DensityRulesClassifier implements TextBlockFilter {
  public static final DensityRulesClassifier INSTANCE = new DensityRulesClassifier();

  /**
   * Returns the singleton instance for RulebasedBoilerpipeClassifier.
   */
  public static DensityRulesClassifier getInstance() {
    return INSTANCE;
  }

  public boolean process(TextDocument doc) throws ProcessingException {
    List<TextBlock> textBlocks = doc.getTextBlocks();
    boolean hasChanges = false;

    ListIterator<TextBlock> it = textBlocks.listIterator();
    if (!it.hasNext()) {
      return false;
    }
    TextBlock prevBlock = TextBlock.EMPTY_START;
    TextBlock currentBlock = it.next();
    TextBlock nextBlock = it.hasNext() ? it.next() : TextBlock.EMPTY_START;

    hasChanges = classify(prevBlock, currentBlock, nextBlock) | hasChanges;

    if (nextBlock != TextBlock.EMPTY_START) {
      while (it.hasNext()) {
        prevBlock = currentBlock;
        currentBlock = nextBlock;
        nextBlock = it.next();
        hasChanges = classify(prevBlock, currentBlock, nextBlock) | hasChanges;
      }
      prevBlock = currentBlock;
      currentBlock = nextBlock;
      nextBlock = TextBlock.EMPTY_START;
      hasChanges = classify(prevBlock, currentBlock, nextBlock) | hasChanges;
    }

    return hasChanges;
  }

  protected boolean classify(final TextBlock prev, final TextBlock curr, final TextBlock next) {
    final boolean isContent;

    if (curr.getLinkDensity() <= 0.333333) {
      if (prev.getLinkDensity() <= 0.555556) {
        if (curr.getTextDensity() <= 9) {
          if (next.getTextDensity() <= 10) {
            isContent = prev.getTextDensity() > 4;
          } else {
            isContent = true;
          }
        } else {
          isContent = next.getTextDensity() != 0;
        }
      } else {
        isContent = next.getTextDensity() > 11;
      }
    } else {
      isContent = false;
    }

    return curr.setIsContent(isContent);
  }

}
