package ai.platon.pulsar.protocol.browser.hotfix.sites.amazon

import ai.platon.pulsar.browser.common.BlockRules

class AmazonBlockRules: BlockRules() {
    /**
     * amazon.com note:
     * The following have to pass, or the site refuses to serve:
     * .woff,
     * .mp4
     * */
    override val mustPassUrls get() = listOf("*.woff", "*.mp4").toMutableList()
}
