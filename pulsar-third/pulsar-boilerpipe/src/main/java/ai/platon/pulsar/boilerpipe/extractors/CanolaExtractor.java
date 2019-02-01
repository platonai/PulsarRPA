package ai.platon.pulsar.boilerpipe.extractors;

import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

import java.util.List;
import java.util.ListIterator;

/**
 * A full-text extractor trained on <a href="http://krdwrd.org/">krdwrd</a> <a href
 * ="https://krdwrd.org/trac/attachment/wiki/Corpora/Canola/CANOLA.pdf">Canola </a>.
 */
public class CanolaExtractor implements TextExtractor {
  public static final CanolaExtractor INSTANCE = new CanolaExtractor();

  /**
   * Returns the singleton instance for {@link CanolaExtractor}.
   */
  public static CanolaExtractor getInstance() {
    return INSTANCE;
  }

  public boolean process(TextDocument doc) throws ProcessingException {

    return CLASSIFIER.process(doc);
  }

  /**
   * The actual classifier, exposed.
   */
  public static final TextBlockFilter CLASSIFIER = new TextBlockFilter() {

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
      final boolean isContent =
          (curr.getLinkDensity() > 0 && next.getNumWords() > 11)
              || (curr.getNumWords() > 19 || (next.getNumWords() > 6 && next.getLinkDensity() == 0
              && prev.getLinkDensity() == 0 && (curr.getNumWords() > 6
              || prev.getNumWords() > 7 || next.getNumWords() > 19)));

      return curr.setIsContent(isContent);
    }
  };
}
