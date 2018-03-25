package fun.platonic.pulsar.boilerpipe.filters.heuristics;

import fun.platonic.pulsar.boilerpipe.document.BlockLabels;
import fun.platonic.pulsar.boilerpipe.document.TextBlock;
import fun.platonic.pulsar.boilerpipe.document.TextDocument;
import fun.platonic.pulsar.boilerpipe.filters.TextBlockFilter;
import fun.platonic.pulsar.boilerpipe.utils.ProcessingException;

import java.util.Iterator;

/**
 * Marks all blocks as "non-content" that occur after blocks that have been marked
 * {@link BlockLabels#INDICATES_END_OF_TEXT}. These marks are ignored unless a minimum number of
 * words in content blocks occur before this mark (default: 60). This can be used in conjunction
 * with an upstream {@link TerminatingBlocksFinder}.
 *
 * @see TerminatingBlocksFinder
 */
public final class IgnoreBlocksAfterContentFilter extends HeuristicFilterBase implements TextBlockFilter {
  public static final IgnoreBlocksAfterContentFilter DEFAULT_INSTANCE = new IgnoreBlocksAfterContentFilter(60);
  public static final IgnoreBlocksAfterContentFilter INSTANCE_200 = new IgnoreBlocksAfterContentFilter(200);
  private final int minNumWords;

  /**
   * Returns the singleton instance for DeleteBlocksAfterContentFilter.
   */
  public static IgnoreBlocksAfterContentFilter getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  public IgnoreBlocksAfterContentFilter(final int minNumWords) {
    this.minNumWords = minNumWords;
  }

  public boolean process(TextDocument doc) throws ProcessingException {
    boolean changes = false;

    int numWords = 0;
    boolean foundEndOfText = false;
    for (Iterator<TextBlock> it = doc.getTextBlocks().iterator(); it.hasNext(); ) {
      TextBlock block = it.next();

      final boolean endOfText = block.hasLabel(BlockLabels.INDICATES_END_OF_TEXT);
      if (block.isContent()) {
        numWords += getNumFullTextWords(block);
      }

      if (endOfText && numWords >= minNumWords) {
        foundEndOfText = true;
      }

      if (foundEndOfText) {
        changes = true;
        block.setIsContent(false);
      }
    }

    return changes;
  }
}
