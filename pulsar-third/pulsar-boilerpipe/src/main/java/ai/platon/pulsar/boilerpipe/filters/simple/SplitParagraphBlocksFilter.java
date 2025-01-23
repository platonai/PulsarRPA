package ai.platon.pulsar.boilerpipe.filters.simple;

import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.BoiTextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Splits TextBlocks at paragraph boundaries.
 *
 * NOTE: This is not fully supported (i.e., it will break highlighting support via
 * #getContainedTextElements()), but this one probably is necessary for some other filters.
 *
 * @see MinClauseWordsFilter
 */
public final class SplitParagraphBlocksFilter implements TextBlockFilter {
  public static final SplitParagraphBlocksFilter INSTANCE = new SplitParagraphBlocksFilter();

  /**
   * Returns the singleton instance for TerminatingBlocksFinder.
   */
  public static SplitParagraphBlocksFilter getInstance() {
    return INSTANCE;
  }

  public boolean process(BoiTextDocument doc) throws ProcessingException {
    boolean changes = false;

    final List<TextBlock> blocks = doc.getTextBlocks();
    final List<TextBlock> blocksNew = new ArrayList<TextBlock>();

    for (TextBlock tb : blocks) {
      final String text = tb.getText();
      final String[] paragraphs = text.split("[\n\r]+");
      if (paragraphs.length < 2) {
        blocksNew.add(tb);
        continue;
      }
      final boolean isContent = tb.isContent();
      final Set<String> labels = tb.getLabels();
      for (String p : paragraphs) {
        final TextBlock tbP = new TextBlock(p);
        tbP.setIsContent(isContent);
        tbP.addLabels(labels);
        blocksNew.add(tbP);
        changes = true;
      }
    }

    if (changes) {
      blocks.clear();
      blocks.addAll(blocksNew);
    }

    return changes;
  }

}
