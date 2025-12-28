package ai.platon.pulsar.examples.sites.jd;

import ai.platon.pulsar.skeleton.context.PulsarContexts;

class JdCrawler {

    public static void main(String[] argv) throws Exception {
        var portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349";
        var args = "-i 1s -ii 5s -ol a[href~=item] -ignoreFailure";
        try (var session = PulsarContexts.createSession()) {
            session.load(portalUrl);
            session.loadOutPages(portalUrl, args);
        }
    }
}
