package ai.platon.pulsar.boilerpipe;

import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.extractors.ChineseNewsExtractor;
import ai.platon.pulsar.boilerpipe.sax.HTMLDownloader;
import ai.platon.pulsar.boilerpipe.sax.HTMLParser;
import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.common.ResourceLoader;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Created by vincent on 16-11-9.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestExtractDateTime {

    private final int LIMIT = 20;

    @Test
    public void extract() throws IOException {
        List<String> urls = ResourceLoader.INSTANCE.readAllLines("urls-detail-FetchJob-1022.100818.txt.sorted");
        urls.stream()
                .filter(line -> !line.contains("house"))
                .limit(LIMIT)
                .map(line -> "http" + StringUtils.substringAfterLast(line, "http"))
                .forEach(this::extractOneNoThrow);
    }

    private void extractOneNoThrow(String url) {
        try {
            extractOne(url);
        } catch (Exception e) {
            System.out.println("Failed to extract " + url);
        }
    }

    private void extractOne(String url) throws Exception {
        String html = HTMLDownloader.fetch(url);
        if (html == null) {
            System.out.println("Failed to fetch url " + url);
            return;
        }

        // TextDocument doc = new SAXInput(is, url).getTextDocument();
        HTMLParser parser = new HTMLParser(url);
        try {
            parser.parse(html);
        } catch (Throwable e) {
            System.out.println(StringUtils.substring(e.toString(), 0, 100));
            return;
        }
        TextDocument doc = parser.getTextDocument();

        ChineseNewsExtractor.INSTANCE.process(doc);

        if (doc.getTextContent().length() > 0) {
            if (doc.getModifiedTime().isAfter(Instant.EPOCH)) {
                System.out.println("v\t"
                        + DateTimes.isoInstantFormat(doc.getPublishTime())
                        + ", " + DateTimes.isoInstantFormat(doc.getModifiedTime())
                        + ", " + doc.getPageCategory()
                        + "\t<-\t" + url);
            } else {
                System.out.println("x\t" + url + "\t" + StringUtils.substring(doc.getTextContent(), 0, 100).replaceAll("\n", "\\n"));
            }
        }
    }
}
