package ai.platon.pulsar.boilerpipe.filters;

import ai.platon.pulsar.boilerpipe.utils.ProcessingException;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

/**
 * A generic {@link TextBlockFilter}. Takes a {@link TextDocument} and processes it somehow.
 */
public interface TextBlockFilter {
  /**
   * Processes the given document <code>doc</code>.
   *
   * @param doc The {@link TextDocument} that is to be processed.
   * @return <code>true</code> if changes have been made to the {@link TextDocument}.
   * @throws ProcessingException
   */
  boolean process(final TextDocument doc) throws ProcessingException;
}
