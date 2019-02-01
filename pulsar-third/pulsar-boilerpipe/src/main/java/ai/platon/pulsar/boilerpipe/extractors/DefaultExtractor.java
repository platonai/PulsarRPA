package ai.platon.pulsar.boilerpipe.extractors;

import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.heuristics.BlockProximityFusion;
import ai.platon.pulsar.boilerpipe.filters.heuristics.SimpleBlockFusionProcessor;
import ai.platon.pulsar.boilerpipe.filters.statistics.DensityRulesClassifier;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

/**
 * A quite generic full-text extractor.
 */
public class DefaultExtractor implements TextExtractor {
  public static final DefaultExtractor INSTANCE = new DefaultExtractor();

  /**
   * Returns the singleton instance for {@link DefaultExtractor}.
   */
  public static DefaultExtractor getInstance() {
    return INSTANCE;
  }

  public boolean process(TextDocument doc) throws ProcessingException {

    return
        SimpleBlockFusionProcessor.INSTANCE.process(doc)
            | BlockProximityFusion.MAX_DISTANCE_1.process(doc)
            | DensityRulesClassifier.INSTANCE.process(doc);
  }
}
