package fun.platonic.pulsar.boilerpipe.sax;

import fun.platonic.pulsar.boilerpipe.document.LabelAction;
import fun.platonic.pulsar.boilerpipe.document.TextBlock;
import fun.platonic.pulsar.boilerpipe.document.TextDocument;
import fun.platonic.pulsar.boilerpipe.utils.PageCategory;
import fun.platonic.pulsar.boilerpipe.utils.UnicodeTokenizer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A simple SAX {@link ContentHandler}, used by {@link SAXInput}. Can be used by different
 * parser implementations, e.g. NekoHTML and TagSoup.
 */
public class HTMLContentHandler implements ContentHandler {

  private final Map<String, TagAction> tagActions;
  private String baseUrl = null;
  private String pageTitle = null;

  static final String ANCHOR_TEXT_START = "$\ue00a<";
  static final String ANCHOR_TEXT_END = ">\ue00a$";

  StringBuilder tokenBuffer = new StringBuilder();
  StringBuilder textBuffer = new StringBuilder();

  int inBody = 0;
  int inAnchor = 0;
  int inIgnorableElement = 0;

  int tagLevel = 0;
  int blockTagLevel = -1;

  boolean sbLastWasWhitespace = false;
  private int textElementIdx = 0;

  private final List<TextBlock> textBlocks = new ArrayList<>();

  private String lastStartTag = null;
  @SuppressWarnings("unused")
  private String lastEndTag = null;
  @SuppressWarnings("unused")
  private Event lastEvent = null;

  private int offsetBlocks = 0;
  private BitSet currentContainedTextElements = new BitSet();

  private boolean flush = false;
  boolean inAnchorText = false;

  /** The number of characters, the naming conversion is pulsar compatible */
  int _char = 0;
  /** The number of links, the naming conversion is pulsar compatible */
  int _a = 0;

  LinkedList<LinkedList<LabelAction>> labelStacks = new LinkedList<>();
  LinkedList<Integer> fontSizeStack = new LinkedList<>();

  /**
   * Constructs a {@link HTMLContentHandler} using the {@link DefaultTagActionMap}.
   */
  public HTMLContentHandler(String baseUrl) {
    this(baseUrl, DefaultTagActionMap.INSTANCE);
  }

  /**
   * Constructs a {@link HTMLContentHandler} using the given {@link TagActionMap}.
   *
   * @param tagActions The {@link TagActionMap} to use, e.g. {@link DefaultTagActionMap}.
   */
  public HTMLContentHandler(String baseUrl, TagActionMap tagActions) {
    this.baseUrl = baseUrl;
    this.tagActions = tagActions;
  }

  @Override
  public void endDocument() throws SAXException {
    flushBlock();
  }

  @Override
  public void endPrefixMapping(String prefix) throws SAXException {
  }

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    if (!sbLastWasWhitespace) {
      textBuffer.append(' ');
      tokenBuffer.append(' ');
    }
    sbLastWasWhitespace = true;
  }

  @Override
  public void processingInstruction(String target, String data) throws SAXException {
  }

  @Override
  public void setDocumentLocator(Locator locator) {
  }

  @Override
  public void skippedEntity(String name) throws SAXException {
  }

  @Override
  public void startDocument() throws SAXException {
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    labelStacks.add(null);

    TagAction ta = tagActions.get(localName);
    if (ta != null) {
      if (ta.changesTagLevel()) {
        tagLevel++;
      }
      flush = ta.start(this, localName, qName, atts) | flush;
    } else {
      tagLevel++;
      flush = true;
    }

    lastEvent = Event.START_TAG;
    lastStartTag = localName;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    TagAction ta = tagActions.get(localName);
    if (ta != null) {
      flush = ta.end(this, localName, qName) | flush;
    } else {
      flush = true;
    }

    if (ta == null || ta.changesTagLevel()) {
      tagLevel--;
    }

    if (flush) {
      flushBlock();
    }

    lastEvent = Event.END_TAG;
    lastEndTag = localName;

    labelStacks.removeLast();
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    textElementIdx++;

    if (flush) {
      flushBlock();
      flush = false;
    }

    if (inIgnorableElement != 0) {
      return;
    }

    char c;
    boolean startWhitespace = false;
    boolean endWhitespace = false;
    if (length == 0) {
      return;
    }

    final int end = start + length;
    for (int i = start; i < end; i++) {
      if (Character.isWhitespace(ch[i])) {
        ch[i] = ' ';
      }
    }
    while (start < end) {
      c = ch[start];
      if (c == ' ') {
        startWhitespace = true;
        start++;
        length--;
      } else {
        break;
      }
    }
    while (length > 0) {
      c = ch[start + length - 1];
      if (c == ' ') {
        endWhitespace = true;
        length--;
      } else {
        break;
      }
    }
    if (length == 0) {
      if (startWhitespace || endWhitespace) {
        if (!sbLastWasWhitespace) {
          textBuffer.append(' ');
          tokenBuffer.append(' ');
        }
        sbLastWasWhitespace = true;
      } else {
        sbLastWasWhitespace = false;
      }
      lastEvent = Event.WHITESPACE;
      return;
    }

    if (startWhitespace) {
      if (!sbLastWasWhitespace) {
        textBuffer.append(' ');
        tokenBuffer.append(' ');
      }
    }

    if (blockTagLevel == -1) {
      blockTagLevel = tagLevel;
    }

    textBuffer.append(ch, start, length);
    tokenBuffer.append(ch, start, length);
    if (endWhitespace) {
      textBuffer.append(' ');
      tokenBuffer.append(' ');
    }

    sbLastWasWhitespace = endWhitespace;
    lastEvent = Event.CHARACTERS;

    _char += length;

    currentContainedTextElements.set(textElementIdx);
  }

  List<TextBlock> getTextBlocks() {
    return textBlocks;
  }

  public void flushBlock() {
    if (inBody == 0) {
      if ("TITLE".equalsIgnoreCase(lastStartTag)) {
        // TODO : Why we enter the tag for 3 times?
        if (pageTitle == null || pageTitle.isEmpty()) {
          pageTitle = tokenBuffer.toString().trim();
        }
      }
      textBuffer.setLength(0);
      tokenBuffer.setLength(0);
      return;
    }

    final int length = tokenBuffer.length();
    switch (length) {
      case 0:
        return;
      case 1:
        if (sbLastWasWhitespace) {
          textBuffer.setLength(0);
          tokenBuffer.setLength(0);
          return;
        }
    }
    final String[] tokens = UnicodeTokenizer.tokenize(tokenBuffer);

    int numWords = 0;
    int numLinkedWords = 0;
    int numWrappedLines = 0;
    int currentLineLength = -1; // don't count the first space
    final int maxLineLength = 80;
    int numTokens = 0;
    int numWordsCurrentLine = 0;

    for (String token : tokens) {
      if (ANCHOR_TEXT_START.equals(token)) {
        inAnchorText = true;
      } else if (ANCHOR_TEXT_END.equals(token)) {
        inAnchorText = false;
      } else if (isWord(token)) {
        numTokens++;
        numWords++;
        numWordsCurrentLine++;
        if (inAnchorText) {
          numLinkedWords++;
        }
        final int tokenLength = token.length();
        currentLineLength += tokenLength + 1;
        if (currentLineLength > maxLineLength) {
          numWrappedLines++;
          currentLineLength = tokenLength;
          numWordsCurrentLine = 1;
        }
      } else {
        numTokens++;
      }
    }

    if (numTokens == 0) {
      return;
    }

    int numWordsInWrappedLines;
    if (numWrappedLines == 0) {
      numWordsInWrappedLines = numWords;
      numWrappedLines = 1;
    } else {
      numWordsInWrappedLines = numWords - numWordsCurrentLine;
    }

    TextBlock tb =
        new TextBlock(textBuffer.toString().trim(), currentContainedTextElements, numWords,
            numLinkedWords, numWordsInWrappedLines, numWrappedLines, offsetBlocks);

    tb.setCssSelector(lastStartTag);

    currentContainedTextElements = new BitSet();

    offsetBlocks++;

    textBuffer.setLength(0);
    tokenBuffer.setLength(0);

    tb.setTagLevel(blockTagLevel);
    addTextBlock(tb);
    blockTagLevel = -1;
  }

  protected void addTextBlock(final TextBlock tb) {
    for (Integer l : fontSizeStack) {
      if (l != null) {
        tb.addLabel("font-" + l);
        break;
      }
    }

    for (LinkedList<LabelAction> labelStack : labelStacks) {
      if (labelStack != null) {
        for (LabelAction labels : labelStack) {
          if (labels != null) {
            labels.addTo(tb);
          }
        }
      }
    }

    textBlocks.add(tb);
  }

  private static final Pattern PAT_VALID_WORD_CHARACTER = Pattern.compile("[\\p{L}\\p{Nd}\\p{Nl}\\p{No}]");

  private static boolean isWord(final String token) {
    return PAT_VALID_WORD_CHARACTER.matcher(token).find();
  }

  private enum Event {
    START_TAG, END_TAG, CHARACTERS, WHITESPACE
  }

  public String getPageTitle() { return this.pageTitle; }

  public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }

  public String getBaseUrl() { return this.baseUrl; }

  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

  /**
   * Returns a {@link TextDocument} containing the extracted {@link TextBlock} s. NOTE: Only call
   * this after parsing.
   *
   * @return The {@link TextDocument}
   */
  public TextDocument toTextDocument() {
    // just to be sure
    flushBlock();

    TextDocument doc = new TextDocument(baseUrl, pageTitle, getTextBlocks());
    doc.getStat()._char = _char;
    doc.getStat()._a = _a;
    doc.setPageCategory(PageCategory.sniff(baseUrl, _char, _a));

    return doc;
  }

  public void addWhitespaceIfNecessary() {
    if (!sbLastWasWhitespace) {
      tokenBuffer.append(' ');
      textBuffer.append(' ');
      sbLastWasWhitespace = true;
    }
  }

  public void addLabelAction(final LabelAction la) throws IllegalStateException {
    LinkedList<LabelAction> labelStack = labelStacks.getLast();
    if (labelStack == null) {
      labelStack = new LinkedList<>();
      labelStacks.removeLast();
      labelStacks.add(labelStack);
    }
    labelStack.add(la);
  }
}
