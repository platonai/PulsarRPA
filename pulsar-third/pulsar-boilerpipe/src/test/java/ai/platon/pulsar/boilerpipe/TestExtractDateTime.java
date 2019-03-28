package ai.platon.pulsar.boilerpipe;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.extractors.ChineseNewsExtractor;
import ai.platon.pulsar.boilerpipe.sax.HTMLDownloader;
import ai.platon.pulsar.boilerpipe.sax.HTMLParser;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * Created by vincent on 16-11-9.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestExtractDateTime {

    private final int LIMIT = 20;
    private final static String SAMPLES_DIR = System.getProperty("test.data", ".");

    @Test
    public void extract() throws IOException {
        Path path = Paths.get(SAMPLES_DIR, "urls-detail-FetchJob-1022.100818.txt.sorted");
        Files.readAllLines(path).stream()
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
                        + DateTimeUtil.isoInstantFormat(doc.getPublishTime())
                        + ", " + DateTimeUtil.isoInstantFormat(doc.getModifiedTime())
                        + ", " + doc.getPageCategory()
                        + "\t<-\t" + url);
            } else {
                System.out.println("x\t" + url + "\t" + StringUtils.substring(doc.getTextContent(), 0, 100).replaceAll("\n", "\\n"));
            }
        }
    }
}
