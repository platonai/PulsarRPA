package ai.platon.pulsar.protocol.browser.conf.sites.amazon

import ai.platon.pulsar.browser.driver.BlockRules

class AmazonBlockRules: BlockRules() {
    /**
     * amazon.com note:
     * The following have to pass, or the site refuses to serve:
     * .woff,
     * .mp4
     * */
    override val mustPassUrls = listOf("*.woff", "*.mp4").toMutableList()
}
