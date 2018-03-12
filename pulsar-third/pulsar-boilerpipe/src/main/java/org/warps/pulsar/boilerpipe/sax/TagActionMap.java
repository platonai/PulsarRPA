package org.warps.pulsar.boilerpipe.sax;

import java.util.HashMap;

/**
 * Base class for definition a set of {@link TagAction}s that are to be used for the HTML parsing
 * process.
 *
 * @see DefaultTagActionMap
 */
public abstract class TagActionMap extends HashMap<String, TagAction> {
  private static final long serialVersionUID = 1L;

  /**
   * Sets a particular {@link TagAction} for a given tag. Any existing TagAction for that tag will
   * be removed and overwritten.
   *
   * @param tag The tag (will be stored internally 1. as it is, 2. lower-case, 3. upper-case)
   * @param action The {@link TagAction}
   */
  protected void setTagAction(final String tag, final TagAction action) {
    put(tag.toUpperCase(), action);
    put(tag.toLowerCase(), action);
    put(tag, action);
  }

  /**
   * Adds a particular {@link TagAction} for a given tag. If a TagAction already exists for that
   * tag, a chained action, consisting of the previous and the new {@link TagAction} is created.
   *
   * @param tag The tag (will be stored internally 1. as it is, 2. lower-case, 3. upper-case)
   * @param action The {@link TagAction}
   */
  protected void addTagAction(final String tag, final TagAction action) {
    TagAction previousAction = get(tag);
    if (previousAction == null) {
      setTagAction(tag, action);
    } else {
      setTagAction(tag, new CommonTagActions.Chained(previousAction, action));
    }
  }
}
