package org.warps.pulsar.boilerpipe.sax;

import org.warps.pulsar.boilerpipe.document.BlockLabels;
import org.warps.pulsar.boilerpipe.document.LabelAction;

/**
 * Default {@link TagAction}s. Seem to work well.
 */
public class DefaultTagActionMap extends TagActionMap {

  public static final TagActionMap INSTANCE = new DefaultTagActionMap();

  protected DefaultTagActionMap() {
    setTagAction("STYLE", CommonTagActions.TA_IGNORABLE_ELEMENT);
    setTagAction("SCRIPT", CommonTagActions.TA_IGNORABLE_ELEMENT);
    setTagAction("OPTION", CommonTagActions.TA_IGNORABLE_ELEMENT);
    setTagAction("OBJECT", CommonTagActions.TA_IGNORABLE_ELEMENT);
    setTagAction("EMBED", CommonTagActions.TA_IGNORABLE_ELEMENT);
    setTagAction("APPLET", CommonTagActions.TA_IGNORABLE_ELEMENT);
    setTagAction("LINK", CommonTagActions.TA_IGNORABLE_ELEMENT);

    setTagAction("A", CommonTagActions.TA_ANCHOR_TEXT);
    setTagAction("BODY", CommonTagActions.TA_BODY);

    setTagAction("STRIKE", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("U", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("B", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("I", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("EM", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("STRONG", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("SPAN", CommonTagActions.TA_INLINE_NO_WHITESPACE);

    // New in 1.1 (especially to improve extraction quality from Wikipedia etc.)
    setTagAction("SUP", CommonTagActions.TA_INLINE_NO_WHITESPACE);

    // New in 1.2
    setTagAction("CODE", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("TT", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("SUB", CommonTagActions.TA_INLINE_NO_WHITESPACE);
    setTagAction("VAR", CommonTagActions.TA_INLINE_NO_WHITESPACE);

    setTagAction("ABBR", CommonTagActions.TA_INLINE_WHITESPACE);
    setTagAction("ACRONYM", CommonTagActions.TA_INLINE_WHITESPACE);

    setTagAction("FONT", CommonTagActions.TA_INLINE_NO_WHITESPACE); // could also use TA_FONT

    // added in 1.1.1
    setTagAction("NOSCRIPT", CommonTagActions.TA_IGNORABLE_ELEMENT);

    // New in 1.3
    setTagAction("LI", new CommonTagActions.BlockTagLabelAction(new LabelAction(BlockLabels.LI)));
    setTagAction("H1", new CommonTagActions.BlockTagLabelAction(new LabelAction(BlockLabels.H1,
        BlockLabels.HEADING)));
    setTagAction("H2", new CommonTagActions.BlockTagLabelAction(new LabelAction(BlockLabels.H2,
        BlockLabels.HEADING)));
    setTagAction("H3", new CommonTagActions.BlockTagLabelAction(new LabelAction(BlockLabels.H3,
        BlockLabels.HEADING)));
  }
}
