package ai.platon.pulsar.examples.sites.jd;

import ai.platon.pulsar.context.PulsarContexts;
import ai.platon.pulsar.session.PulsarSession;

class JdCrawler {

    public static void main(String[] argv) {
        String portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349";
        String args = "-i 1s -ii 5m -ol a[href~=item] -ignoreFailure";
        PulsarSession session = PulsarContexts.createSession();
        session.loadOutPages(portalUrl, args);
    }
}
