package ai.platon.pulsar.examples.experimental;

import ai.platon.pulsar.common.LinkExtractors;
import ai.platon.pulsar.common.urls.NormUrl;
import ai.platon.pulsar.context.PulsarContexts;
import ai.platon.pulsar.session.PulsarSession;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

class CrawlAsync {
    public static void loadAsync() {
        PulsarSession session = PulsarContexts.createSession();
        LinkExtractors.fromResource("seeds10.txt").stream()
                .map(url -> session.normalize(url, session.options("", null), false))
                .map(url -> CompletableFuture.supplyAsync(() -> session.load(url)))
                .map(f -> f.thenApply(p -> session.parse(p, true)))
                .map(CompletableFuture::join)
                .forEach(doc -> System.out.println(doc.getTitle()));
    }

    public static void loadAllAsync() {
        PulsarSession session = PulsarContexts.createSession();
        List<NormUrl> urls = LinkExtractors.fromResource("seeds10.txt").stream()
                .map(url -> session.normalize(url, session.options("", null), false))
                .collect(Collectors.toList());
        session.loadAllAsync(urls).stream()
                .map(f -> f.thenApply(p -> session.parse(p, true)))
                .map(CompletableFuture::join)
                .forEach(doc -> System.out.println(doc.getTitle()));
    }

    public static void main(String[] args) {
        loadAsync();
        loadAllAsync();
    }
}
