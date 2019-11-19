package ai.platon.pulsar.boilerpipe.sax;

import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

/**
 * Parses an {@link InputSource} using SAX and returns a {@link TextDocument}.
 */
public final class SAXInput {

  /**
   * Retrieves the {@link TextDocument} using a default HTML parser.
   */
  public TextDocument parse(String baseUrl, InputSource is) throws ProcessingException {
    HTMLParser parser = new HTMLParser(baseUrl);

    try {
      parser.parse(is);
    } catch (IOException|SAXException e) {
      throw new ProcessingException(e);
    }

    return parser.getTextDocument();
  }

  /**
   * Extracts text from the HTML code given as a String.
   *
   * @param html The HTML code as a String.
   * @return The extracted text.
   * @throws ProcessingException
   */
  public TextDocument parse(String baseUrl, String html) throws ProcessingException {
    return parse(baseUrl, new InputSource(new StringReader(html)));
  }

  /**
   * Extracts text from the HTML code available from the given {@link URL}. NOTE: This method is
   * mainly to be used for show case purposes. If you are going to crawl the Web, consider using
   * {@link #parse(String, InputSource)} instead.
   *
   * @param url The URL pointing to the HTML code.
   * @return The extracted text.
   * @throws ProcessingException
   */
  public TextDocument parse(URL url) throws ProcessingException {
    try {
      return parse(url.toString(), HTMLDownloader.fetch(url));
    } catch (IOException e) {
      throw new ProcessingException(e);
    }
  }

  public TextDocument parse(String url) throws ProcessingException {
    try {
      String html = HTMLDownloader.fetch(url);
      // System.out.println(html);
      return parse(url, html);
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
  public TextDocument parse(String baseUrl, Reader reader) throws ProcessingException {
    return parse(baseUrl, new InputSource(reader));
  }
}
