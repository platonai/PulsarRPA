package ai.platon.pulsar.boilerpipe;

import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.extractors.ChineseNewsExtractor;
import ai.platon.pulsar.boilerpipe.extractors.DefaultExtractor;
import ai.platon.pulsar.boilerpipe.sax.HTMLDownloader;
import ai.platon.pulsar.boilerpipe.sax.SAXInput;
import ai.platon.pulsar.common.ResourceLoader;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Created by vincent on 16-11-9.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestExtract {

    @Test
    public void extract() throws IOException {
        List<String> urls = ResourceLoader.INSTANCE.readAllLines("urls-detail-FetchJob-1022.100818.txt.sorted");

        for (String url : urls) {
            try {
                if (url.contains("house")) {
                    continue;
                }

                if (url.startsWith("http")) {
                    extractVerbose(url);
                }
            } catch (Exception e) {
                System.out.println("Failed to extract " + url);
                e.printStackTrace();
            }
        }
    }

    private void extractVerbose(String url) throws Exception {
        String html;
        try {
            html = HTMLDownloader.fetch(url);
        } catch (Exception e) {
            System.out.println("Failed to fetch url " + StringUtils.substring(url, 0, 100));
            e.printStackTrace();
            return;
        }

        if (html == null) {
            System.out.println("Failed to fetch url " + url);
            return;
        }

        TextDocument doc = new SAXInput().parse(url, html);

        ChineseNewsExtractor.INSTANCE.process(doc);

        if (!doc.getTextContent().isEmpty()) {
            CharSequence author = doc.getField("author");
            CharSequence director = doc.getField("director");
            CharSequence reference = doc.getField("reference");
            System.out.println("Author : " + author + ", Director : " + director + ", Reference : " + reference);
            // System.out.println(doc.getHtmlContent());
        }
    }

    public void extractContent(String url, String html) throws Exception {
        if (html == null) {
            System.out.println("Failed to fetch url " + url);
            return;
        }

        TextDocument doc = new SAXInput().parse(url, html);

        ChineseNewsExtractor.INSTANCE.process(doc);

        System.out.println(doc.getPageTitle());
        System.out.println(doc.getTextContent(true, true));
        System.out.println(doc.getHtmlContent());
        System.out.println("--------------");
        System.out.println("Fields : ");
        System.out.println(doc.getFields());
        System.out.println("--------------");
        System.out.println("DebugString : ");

        if (!doc.getTextContent().isEmpty()) {
            CharSequence author = doc.getField("author");
            CharSequence director = doc.getField("director");
            CharSequence reference = doc.getField("reference");
            System.out.println("Author : " + author + ", Director : " + director + ", Reference : " + reference);
            System.out.println(doc.getHtmlContent());
        }

        System.out.println(doc.getTextContent().length());

        System.out.println(StringUtils.substring(doc.getTextContent(), 0, 100));
        System.out.println("\n\n-----------------\n\n");
        System.out.println(StringUtils.substring(doc.getTextContent(), 0, 100));
        System.out.println(url);
        System.out.println(doc.getTextContent());

        System.out.println(doc.getTextContent(true, true));
        System.out.println(doc.getTextBlocks());
        System.out.println(doc.debugString());

        // Also try other extractors!
//        System.out.println(DefaultExtractor.INSTANCE.getTextContent(url));
//        System.out.println(CommonExtractors.CANOLA_EXTRACTOR.getTextContent(url));
    }

    public static void main(String[] args) throws Exception {
        TestExtract extractor = new TestExtract();

        String url = "https://codeblue.galencentre.org/2024/11/upholding-professionalism-honesty-and-integrity-in-medical-education-a-foundation-for-trustworthy-health-care-dr-aida-bustam/";
        String html = ResourceLoader.INSTANCE.readString("pages/galencentre-org-6be616220e993bb8074084bf71a9cb49.html");
        extractor.extractContent(url, html);
    }
}
