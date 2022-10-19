package ai.platon.pulsar.examples.async;

import ai.platon.pulsar.common.LinkExtractors;
import ai.platon.pulsar.context.PulsarContexts;
import ai.platon.pulsar.dom.FeaturedDocument;
import ai.platon.pulsar.session.PulsarSession;

import java.util.concurrent.CompletableFuture;

class CrawlAsync {

    public static void parallelLoadAll() {
        PulsarSession session = PulsarContexts.createSession();
        LinkExtractors.fromResource("seeds10.txt").parallelStream()
                .map(session::open).map(session::parse)
                .map(FeaturedDocument::guessTitle)
                .forEach(System.out::println);
    }

    public static void loadAllAsync2() {
        PulsarSession session = PulsarContexts.createSession();

        CompletableFuture<?>[] futures = LinkExtractors.fromResource("seeds10.txt").stream()
                .map(url -> url + " -i 1d")
                .map(session::loadAsync)
                .map(f -> f.thenApply(session::parse))
                .map(f -> f.thenApply(FeaturedDocument::guessTitle))
                .map(f -> f.thenAccept(System.out::println))
                .toArray(CompletableFuture<?>[]::new);

        CompletableFuture.allOf(futures).join();
    }

    public static void loadAllAsync3() {
        PulsarSession session = PulsarContexts.createSession();

        CompletableFuture<?>[] futures = session.loadAllAsync(LinkExtractors.fromResource("seeds10.txt")).stream()
                .map(f -> f.thenApply(session::parse))
                .map(f -> f.thenApply(FeaturedDocument::guessTitle))
                .map(f -> f.thenAccept(System.out::println))
                .toArray(CompletableFuture<?>[]::new);

        CompletableFuture.allOf(futures).join();
    }

    public static void loadAllAsync4() {
        PulsarSession session = PulsarContexts.createSession();

        CompletableFuture<?>[] futures = session.loadAllAsync(LinkExtractors.fromResource("seeds10.txt")).stream()
                .map(f -> f.thenApply(session::parse).thenApply(FeaturedDocument::guessTitle).thenAccept(System.out::println))
                .toArray(CompletableFuture<?>[]::new);

        CompletableFuture.allOf(futures).join();
    }

    public static void main(String[] args) {
        parallelLoadAll();
        loadAllAsync2();
        loadAllAsync3();
        loadAllAsync4();
    }
}
