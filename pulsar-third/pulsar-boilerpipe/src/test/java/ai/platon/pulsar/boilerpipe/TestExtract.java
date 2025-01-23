package ai.platon.pulsar.boilerpipe;

import ai.platon.pulsar.boilerpipe.extractors.DefaultExtractor;
import ai.platon.pulsar.boilerpipe.sax.HTMLDownloader;
import ai.platon.pulsar.boilerpipe.sax.SAXInput;
import ai.platon.pulsar.common.AppPaths;
import ai.platon.pulsar.common.ResourceLoader;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by vincent on 16-11-9.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestExtract {

    @Test
    public void extract() throws IOException {
        List<String> urls = ResourceLoader.INSTANCE.readAllLines("urls-detail-FetchJob-1022.100818.txt.sorted");

        urls.stream().filter(url -> url.startsWith("http")).forEach(url -> {
            try {
                fetchAndExtract(url);
            } catch (Exception e) {
                System.out.println("Failed to extract " + url);
            }
        });
    }

    public void extractContent(String url, String html) throws Exception {
        var doc = new SAXInput().parse(url, html);

        var extractor = DefaultExtractor.getInstance();
//        var extractor = ChineseNewsExtractor.getInstance();
//        var extractor = ArticleExtractor.getInstance();

        extractor.process(doc);

        System.out.println("--------------");
        System.out.println("Page Title : ");
        System.out.println(doc.getPageTitle());
        System.out.println("Content Title : ");
        System.out.println(doc.getContentTitle());

        System.out.println("TextContent length : ");
        System.out.println(doc.getTextContent().length());
        var path = AppPaths.INSTANCE.getProcTmpTmp("extract-test." + extractor.getClass().getSimpleName() + ".txt");
        Files.writeString(path, doc.getTextContent());
        System.out.println("TextContent path : " + path);

        System.out.println("--------------");
        System.out.println("DebugString : ");
        if (!doc.getTextContent().isEmpty()) {
            var author = doc.getField("author");
            var director = doc.getField("director");
            var reference = doc.getField("reference");
            System.out.println("Author : " + author + ", Director : " + director + ", Reference : " + reference);
        }
    }

    private void fetchAndExtract(String url) throws Exception {
        String html;
        try {
            html = HTMLDownloader.fetch(url);
        } catch (Exception e) {
            System.out.println("Failed to fetch url " + StringUtils.substring(url, 0, 100));
            return;
        }

        if (html == null) {
            System.out.println("Failed to fetch url " + url);
            return;
        }

        var doc = new SAXInput().parse(url, html);

        DefaultExtractor.INSTANCE.process(doc);

        if (!doc.getTextContent().isEmpty()) {
            var author = doc.getField("author");
            var director = doc.getField("director");
            var reference = doc.getField("reference");
            System.out.println("Author : " + author + ", Director : " + director + ", Reference : " + reference);
        }
    }

    public static void main(String[] args) throws Exception {
        var extractor = new TestExtract();

        var url = "https://codeblue.galencentre.org/2024/11/upholding-professionalism-honesty-and-integrity-in-medical-education-a-foundation-for-trustworthy-health-care-dr-aida-bustam/";
        var html = ResourceLoader.INSTANCE.readString("pages/galencentre-org-6be616220e993bb8074084bf71a9cb49.html");
        extractor.extractContent(url, html);
    }
}
