package ai.platon.pulsar.boilerpipe.sax;

import ai.platon.pulsar.boilerpipe.utils.BoiConstants;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A very simple HTTP/HTML fetcher, really just for demo purposes.
 **/
public class HTMLDownloader {

  private HTMLDownloader() {
  }

  public static String fetch(String url) throws IOException {
    return fetch(new URL(url));
  }

  /**
   * Fetches the document at the given URL, using {@link URLConnection}.
   *
   * @param url The URL to fetch
   * @return The HTML at the given URL
   * @throws IOException If an error occurs
   */
  public static String fetch(final URL url) throws IOException {
      try (CloseableHttpClient client = HttpClients.createDefault()) {
          HttpGet httpget = new HttpGet(url.toString());
          httpget.setHeader("User-Agent", BoiConstants.DEFAULT_USER_AGENT);

          // Create a custom response handler
          ResponseHandler<String> responseHandler = new ResponseHandler<>() {
              @Override
              public String handleResponse(final HttpResponse response) throws IOException {
                  int status = response.getStatusLine().getStatusCode();

                  if (status >= 200 && status < 300) {
                      HttpEntity entity = response.getEntity();

                      Charset charset = null;
                      if (entity != null) {
                          charset = ContentType.get(entity).getCharset();
                      }

                      if (charset == null) {
                          charset = StandardCharsets.UTF_8;
                      }

                      return entity != null ? EntityUtils.toString(entity, charset) : null;
                  } else {
                      throw new ClientProtocolException("Unexpected response status: " + status);
                  }
              }

              private void showContentType(HttpEntity entity) {
                  ContentType contentType = ContentType.get(entity);
                  String mimeType = contentType.getMimeType();
                  Charset charset = contentType.getCharset();

                  System.out.println("Content encoding  = " + entity.getContentEncoding());
                  System.out.println("\nMimeType = " + mimeType);
                  System.out.println("Charset  = " + charset);
              }
          };

          return client.execute(httpget, responseHandler);
      }
  }
}
