package ai.platon.pulsar.examples.sites.jd;

import ai.platon.pulsar.context.PulsarContexts;
import ai.platon.pulsar.session.PulsarSession;

final class JdCrawler {
    private final String portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349";
    private final String args = "-i 1s -ii 5m -ol a[href~=item] -ignoreFailure";
    private final PulsarSession session = PulsarContexts.INSTANCE.createSession();

    void crawl() {
        session.loadOutPages(portalUrl, args);
    }

    public static void main(String[] args) {
        JdCrawler crawler = new JdCrawler();
        crawler.crawl();
    }
}
