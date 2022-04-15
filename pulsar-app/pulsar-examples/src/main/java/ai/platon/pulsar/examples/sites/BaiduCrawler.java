package ai.platon.pulsar.examples.sites;

import ai.platon.pulsar.context.PulsarContexts;
import ai.platon.pulsar.session.PulsarSession;

public class BaiduCrawler {
    public static void main(String[] args) throws Exception {
        String url = "https://www.baidu.com/";
        PulsarSession session = PulsarContexts.createSession();
        session.open(url);
    }
}
