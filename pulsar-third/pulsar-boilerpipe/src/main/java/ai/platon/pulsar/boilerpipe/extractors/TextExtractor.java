package ai.platon.pulsar.boilerpipe.extractors;

import ai.platon.pulsar.boilerpipe.document.BoiTextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.sax.HTMLDownloader;
import ai.platon.pulsar.boilerpipe.sax.SAXInput;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

/**
 * Describes a complete filter pipeline.
 */
public interface TextExtractor extends TextBlockFilter {

  /**
   * Extracts text from the HTML code given as a String.
   *
   * @param html The HTML code as a String.
   * @return The extracted text.
   * @throws ProcessingException
   */
  default String getText(String baseUrl, String html) throws ProcessingException {
    return getText(new SAXInput().parse(baseUrl, html));
  }

  /**
   * Extracts text from the HTML code available from the given {@link InputSource}.
   *
   * @param is The InputSource containing the HTML
   * @return The extracted text.
   * @throws ProcessingException
   */
  default String getText(String baseUrl, InputSource is) throws ProcessingException {
    return getText(new SAXInput().parse(baseUrl, is));
  }

  /**
   * Extracts text from the HTML code available from the given {@link URL}. NOTE: This method is
   * mainly to be used for show case purposes. If you are going to crawl the Web, consider using
   * {@link #getText(String, InputSource)} instead.
   *
   * @param url The URL pointing to the HTML code.
   * @return The extracted text.
   * @throws ProcessingException
   */
  default String getText(URL url) throws ProcessingException {
    try {
      return getText(url.toString(), HTMLDownloader.fetch(url));
    } catch (IOException e) {
      throw new ProcessingException(e);
    }
  }

  /**
   * Extracts text from the HTML code available from the given {@link Reader}.
   *
   * @param baseUrl The baseUrl of the page
   * @param reader The Reader containing the HTML
   * @return The extracted text.
   * @throws ProcessingException
   */
  default String getText(String baseUrl, Reader reader) throws ProcessingException {
    return getText(baseUrl, new InputSource(reader));
  }

  /**
   * Extracts text from the given {@link BoiTextDocument} object.
   *
   * @param doc The {@link BoiTextDocument}.
   * @return The extracted text.
   * @throws ProcessingException
   */
  default String getText(BoiTextDocument doc) throws ProcessingException {
    process(doc);
    return doc.getTextContent();
  }
}
