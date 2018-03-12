package org.warps.pulsar.boilerpipe.sax;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;
import org.warps.pulsar.boilerpipe.document.TextBlock;
import org.warps.pulsar.boilerpipe.document.TextDocument;

/**
 * A simple SAX Parser, used by {@link SAXInput}. The parser uses <a
 * href="http://nekohtml.sourceforge.net/">CyberNeko</a> to getTextDocument HTML content.
 */
public class HTMLParser extends AbstractSAXParser {

  private HTMLContentHandler contentHandler;

  /**
   * Constructs a {@link HTMLParser} using a default HTML content handler.
   */
  public HTMLParser(String baseUrl) {
    this(new HTMLContentHandler(baseUrl));
  }

  /**
   * Constructs a {@link HTMLParser} using the given {@link HTMLContentHandler}.
   *
   * @param contentHandler
   */
  private HTMLParser(HTMLContentHandler contentHandler) {
    super(new HTMLConfiguration());
    setContentHandler(contentHandler);
  }

  public void setContentHandler(HTMLContentHandler contentHandler) {
    this.contentHandler = contentHandler;
    super.setContentHandler(contentHandler);
  }

  public void setContentHandler(org.xml.sax.ContentHandler contentHandler) {
    this.contentHandler = null;
    super.setContentHandler(contentHandler);
  }

  /**
   * Returns a {@link TextDocument} containing the extracted {@link TextBlock} s. NOTE: Only call
   * this after {@link #parse(org.xml.sax.InputSource)}.
   *
   * @return The {@link TextDocument}
   */
  public TextDocument getTextDocument() {
    return contentHandler.toTextDocument();
  }
}
