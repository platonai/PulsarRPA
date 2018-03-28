package fun.platonic.pulsar.boilerpipe;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import fun.platonic.pulsar.boilerpipe.document.TextDocument;
import fun.platonic.pulsar.boilerpipe.extractors.ChineseNewsExtractor;
import fun.platonic.pulsar.boilerpipe.sax.HTMLDownloader;
import fun.platonic.pulsar.boilerpipe.sax.HTMLParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by vincent on 16-11-9.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestExtract {

  private final static String SAMPLES_DIR = System.getProperty("test.data", ".");

  @Test
  public void extract() throws IOException {
    List<String> urls = Lists.newArrayList();
//        urls.add("http://www.chinatimes.cc/movie_comment");

    Path path = Paths.get(SAMPLES_DIR, "urls-detail-FetchJob-1022.100818.txt.sorted");
    urls.addAll(Files.readAllLines(path));

    for (String url : urls) {
      try {
        if (url.contains("house")) {
          continue;
        }
        if (url.startsWith("http")) {
          extractOne(url);
        }
      } catch (Exception e) {
        System.out.println("Failed to extract " + url);
      }
    }
  }

  public void extractOne(String url) throws Exception {
    String html;
    try {
      html = HTMLDownloader.fetch(url);
    }
    catch (Exception e) {
      System.out.println("Failed to fetch url " + StringUtils.substring(url, 0, 100));
      return;
    }

    if (html == null) {
      System.out.println("Failed to fetch url " + url);
      return;
    }

    // TextDocument doc = new SAXInput(is, url).getTextDocument();
    HTMLParser parser = new HTMLParser(url);
    parser.parse(html);
    TextDocument doc = parser.getTextDocument();

//    System.out.println("\n\n\n\n");
//    System.out.println(doc.getTextBlocks());
//

    ChineseNewsExtractor.INSTANCE.process(doc);
    // new ChineseNewsExtractor().process(doc);

//    download(url);

//      System.out.println(doc.getPageTitle());
//      System.out.println(doc.getTextContent(true, true));
//      System.out.println(doc.getHtmlContent());
//    System.out.println("--------------");
//    System.out.println("Fields : ");
//    System.out.println(doc.getFields());
//    System.out.println("--------------");
//    System.out.println("DebugString : ");

    if (!doc.getTextContent().isEmpty()) {
      CharSequence author = doc.getField("author");
      CharSequence director = doc.getField("director");
      CharSequence reference = doc.getField("reference");
      System.out.println("Author : " + author + ", Director : " + director + ", Reference : " + reference);
      // System.out.println(doc.getHtmlContent());
    }

//    System.out.println(doc.getTextContent().length());

    // System.out.println(StringUtils.substring(doc.getTextContent(), 0, 100));
//    System.out.println("\n\n-----------------\n\n");
////    System.out.println(StringUtils.substring(doc.getTextContent(), 0, 100));
//    System.out.println(url);
//    System.out.println(doc.getTextContent());

    // System.out.println(doc.getTextContent(true, true));
    // System.out.println(doc.getTextBlocks());
    // System.out.println(doc.debugString());

    // Also try other extractors!
    // System.out.println(DefaultExtractor.INSTANCE.getTextContent(url));
    // System.out.println(CommonExtractors.CANOLA_EXTRACTOR.getTextContent(url));
  }
}
