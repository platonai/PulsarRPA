package ai.platon.pulsar.examples;

import ai.platon.pulsar.common.LinkExtractors;
import ai.platon.pulsar.common.urls.Hyperlink;
import ai.platon.pulsar.context.PulsarContext;
import ai.platon.pulsar.context.PulsarContexts;
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink;
import ai.platon.pulsar.persist.WebPage;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.stream.Collectors;

public class ContinuousCrawler {

    private static void onParse(WebPage page, Document document) {
        // do something wonderful with the document
        System.out.println(document.title() + "\t|\t" + document.baseUri());
    }

    public static void main(String[] args) {
        List<Hyperlink> urls = LinkExtractors.fromResource("seeds.txt")
                .stream()
                .map(seed -> new ParsableHyperlink(seed, ContinuousCrawler::onParse))
                .collect(Collectors.toList());
        PulsarContext context = PulsarContexts.create().submitAll(urls);
        // feel free to submit millions of urls here
        // ...
        context.await();
    }
}
