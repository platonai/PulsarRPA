package org.warps.pulsar.boilerpipe.filters.simple;

import org.warps.pulsar.boilerpipe.document.TextDocument;
import org.warps.pulsar.boilerpipe.filters.TextBlockFilter;
import org.warps.pulsar.boilerpipe.utils.ProcessingException;

import java.io.PrintWriter;

/**
 * Prints debug information about the current state of the TextDocument. (= calls
 * {@link TextDocument#debugString()}.
 */
public final class PrintDebugFilter implements TextBlockFilter {
  /**
   * Returns the default instance for {@link PrintDebugFilter}, which dumps debug information to
   * <code>System.out</code>
   */
  public static final PrintDebugFilter INSTANCE = new PrintDebugFilter(new PrintWriter(System.out,
      true));
  private final PrintWriter out;

  /**
   * Returns the default instance for {@link PrintDebugFilter}, which dumps debug information to
   * <code>System.out</code>
   */
  public static PrintDebugFilter getInstance() {
    return INSTANCE;
  }

  /**
   * Creates a new instance of {@link PrintDebugFilter}.
   *
   * Only use this method if you are not going to dump the debug information to
   * <code>System.out</code> -- for this case, use {@link #getInstance()} instead.
   *
   * @param out The target {@link PrintWriter}. Will not be closed
   */
  public PrintDebugFilter(final PrintWriter out) {
    this.out = out;

  }

  @Override
  public boolean process(TextDocument doc) throws ProcessingException {
    out.println(doc.debugString());

    return false;
  }
}
