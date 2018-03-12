package org.warps.pulsar.boilerpipe.extractors;

import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.heuristics.BlockProximityFusion;
import org.warps.pulsar.boilerpipe.filters.heuristics.SimpleBlockFusionProcessor;
import org.warps.pulsar.boilerpipe.filters.statistics.DensityRulesClassifier;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

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
