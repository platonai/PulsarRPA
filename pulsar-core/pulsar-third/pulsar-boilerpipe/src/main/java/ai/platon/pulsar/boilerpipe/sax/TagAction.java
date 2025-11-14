package ai.platon.pulsar.boilerpipe.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Defines an action that is to be performed whenever a particular tag occurs during HTML parsing.
 */
public interface TagAction {

  boolean start(final HTMLContentHandler instance, final String localName, final String qName, final Attributes atts) throws SAXException;

  boolean end(final HTMLContentHandler instance, final String localName, final String qName) throws SAXException;

  boolean changesTagLevel();
}