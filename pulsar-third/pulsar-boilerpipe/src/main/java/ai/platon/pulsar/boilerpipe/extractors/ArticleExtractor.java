package ai.platon.pulsar.boilerpipe.extractors;

import ai.platon.pulsar.boilerpipe.document.BoiTextDocument;
import ai.platon.pulsar.boilerpipe.filters.heuristics.*;
import ai.platon.pulsar.boilerpipe.filters.simple.BoilerplateBlockFilter;
import ai.platon.pulsar.boilerpipe.filters.statistics.NumWordsRulesClassifier;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

/**
 * A full-text extractor which is tuned towards news articles. In this scenario it achieves higher
 * accuracy than {@link DefaultExtractor}.
 */
public final class ArticleExtractor implements TextExtractor {
  public static final ArticleExtractor INSTANCE = new ArticleExtractor();

  /**
   * Returns the singleton instance for {@link ArticleExtractor}.
   */
  public static ArticleExtractor getInstance() {
    return INSTANCE;
  }

  public boolean process(BoiTextDocument doc) throws ProcessingException {
    return

        TerminatingBlocksFinder.INSTANCE.process(doc)
            | new DocumentTitleMatchClassifier(doc.getPageTitle()).process(doc)
            | NumWordsRulesClassifier.INSTANCE.process(doc)
            | IgnoreBlocksAfterContentFilter.DEFAULT_INSTANCE.process(doc)
            | TrailingHeadlineToBoilerplateFilter.INSTANCE.process(doc)
            | BlockProximityFusion.MAX_DISTANCE_1.process(doc)
            | BoilerplateBlockFilter.INSTANCE_KEEP_TITLE.process(doc)
            | BlockProximityFusion.MAX_DISTANCE_1_CONTENT_ONLY_SAME_TAGLEVEL.process(doc)
            | KeepLargestBlockFilter.INSTANCE_EXPAND_TO_SAME_TAGLEVEL_MIN_WORDS.process(doc)
            | ExpandTitleToContentFilter.INSTANCE.process(doc)
            | LargeBlockSameTagLevelToContentFilter.INSTANCE.process(doc)
            | ListAtEndFilter.INSTANCE.process(doc);
  }
}
