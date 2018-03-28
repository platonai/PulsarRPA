/**
 * Created by vincent on 16-11-9.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
package fun.platonic.pulsar.boilerpipe.document;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public class TextBlock implements Cloneable {

  public static final BitSet EMPTY_BITSET = new BitSet();
  public static final TextBlock EMPTY_START = new TextBlock("", EMPTY_BITSET, 0, 0, 0, 0, -1);
  public static final TextBlock EMPTY_END = new TextBlock("", EMPTY_BITSET, 0, 0, 0, 0, Integer.MAX_VALUE);

  private boolean isContent = false;
  private CharSequence text;
  private CharSequence richText;
  private Set<String> labels = new HashSet<>(2);

  private int offsetBlocksStart;
  private int offsetBlocksEnd;

  private int numWords;
  private int numWordsInAnchorText;
  private int numWordsInWrappedLines;
  private int numWrappedLines;
  private float textDensity;
  private float linkDensity;

  private BitSet containedTextElements;

  private int numFullTextWords = 0;
  private int tagLevel;

  /** Reserved */
  private String cssSelector;

  public TextBlock(final String text) {
    this(text, null, 0, 0, 0, 0, 0);
  }

  public TextBlock(final String text, final BitSet containedTextElements, final int numWords,
                   final int numWordsInAnchorText, final int numWordsInWrappedLines, final int numWrappedLines,
                   final int offsetBlocks) {
    this.text = text;
    this.richText = text.length() > 1 ? "<p>" + text + "</p>\n" : "";
    this.containedTextElements = containedTextElements;
    this.numWords = numWords;
    this.numWordsInAnchorText = numWordsInAnchorText;
    this.numWordsInWrappedLines = numWordsInWrappedLines;
    this.numWrappedLines = numWrappedLines;
    this.offsetBlocksStart = offsetBlocks;
    this.offsetBlocksEnd = offsetBlocks;
    initDensities();
  }

  public boolean isContent() {
    return isContent;
  }

  public boolean setIsContent(boolean isContent) {
    if (isContent != this.isContent) {
      this.isContent = isContent;
      return true;
    } else {
      return false;
    }
  }

  public String getText() {
    return text.toString();
  }

  public String getRichText() {
    return richText.toString();
  }

  public int getNumWords() {
    return numWords;
  }

  public int getNumWordsInAnchorText() {
    return numWordsInAnchorText;
  }

  public float getTextDensity() {
    return textDensity;
  }

  public float getLinkDensity() {
    return linkDensity;
  }

  /** We support the most simple css selector only currently */
  public String getCssSelector() { return cssSelector; }

  public void setCssSelector(String cssSelector) { this.cssSelector = cssSelector; }

  public void mergeNext(final TextBlock other) {
    if (!(text instanceof StringBuilder)) {
      text = new StringBuilder(text);
    }

    if (!(richText instanceof StringBuilder)) {
      richText = new StringBuilder(richText);
    }

    StringBuilder sb = (StringBuilder) text;
    StringBuilder richSb = (StringBuilder) richText;

    String copy = other.text.toString();

    sb.append('\n');
    sb.append(copy);

    if (copy.length() > 1) {
      richSb.append("<p>");
      richSb.append(copy);
      richSb.append("</p>\n");
    }

    numWords += other.numWords;
    numWordsInAnchorText += other.numWordsInAnchorText;

    numWordsInWrappedLines += other.numWordsInWrappedLines;
    numWrappedLines += other.numWrappedLines;

    offsetBlocksStart = Math.min(offsetBlocksStart, other.offsetBlocksStart);
    offsetBlocksEnd = Math.max(offsetBlocksEnd, other.offsetBlocksEnd);

    initDensities();

    this.isContent |= other.isContent;

    if (containedTextElements == null) {
      containedTextElements = (BitSet) other.containedTextElements.clone();
    } else {
      containedTextElements.or(other.containedTextElements);
    }

    numFullTextWords += other.numFullTextWords;

    if (other.labels != null) {
      if (labels == null) {
        labels = new HashSet<>(other.labels);
      } else {
        labels.addAll(other.labels);
      }
    }

    tagLevel = Math.min(tagLevel, other.tagLevel);
  }

  private void initDensities() {
    if (numWordsInWrappedLines == 0) {
      numWordsInWrappedLines = numWords;
      numWrappedLines = 1;
    }
    textDensity = numWordsInWrappedLines / (float) numWrappedLines;
    linkDensity = numWords == 0 ? 0 : numWordsInAnchorText / (float) numWords;
  }

  public int getOffsetBlocksStart() {
    return offsetBlocksStart;
  }

  public int getOffsetBlocksEnd() {
    return offsetBlocksEnd;
  }

  public String toString() {
    return "[" + offsetBlocksStart + "-" + offsetBlocksEnd + ";tl=" + tagLevel + "; nw=" + numWords
        + ";nwl=" + numWrappedLines + ";ld=" + linkDensity + "]\t"
        + "selector=" + getCssSelector() + ";\t"
        + (isContent ? "CONTENT" : "boilerplate") + "," + labels + "\n" + getText();
  }

  public void addLabel(final String label) {
    labels.add(label);
  }

  public boolean hasLabel(final String label) {
    return labels != null && labels.contains(label);
  }

  public boolean removeLabel(final String label) {
    return labels != null && labels.remove(label);
  }

  public Set<String> getLabels() {
    return labels;
  }

  public void addLabels(final Set<String> l) {
    if (l == null) {
      return;
    }
    if (this.labels == null) {
      this.labels = new HashSet<>(l);
    } else {
      this.labels.addAll(l);
    }
  }

  public void addLabels(final String... l) {
    if (l == null) {
      return;
    }
    if (this.labels == null) {
      this.labels = new HashSet<>();
    }
    for (final String label : l) {
      this.labels.add(label);
    }
  }

  public BitSet getContainedTextElements() {
    return containedTextElements;
  }

  @Override
  protected TextBlock clone() {
    final TextBlock clone;
    try {
      clone = (TextBlock) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    if (text != null && !(text instanceof String)) {
      clone.text = new StringBuilder(text);
    }
    if (labels != null && !labels.isEmpty()) {
      clone.labels = new HashSet<String>(labels);
    }
    if (containedTextElements != null) {
      clone.containedTextElements = (BitSet) containedTextElements.clone();
    }

    return clone;
  }

  public int getTagLevel() {
    return tagLevel;
  }

  public void setTagLevel(int tagLevel) {
    this.tagLevel = tagLevel;
  }
}
