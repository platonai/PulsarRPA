package ai.platon.pulsar.protocol.browser.hotfix.sites.jd

class JdEmulator {
    val expressions = listOf(
        "__pulsar_utils__.click('#summary-service a')",
        "__pulsar_utils__.click('#ns_services a')",
        "__pulsar_utils__.click('.summary-price a')",
        "__pulsar_utils__.click('#comment-count a')",
        "__pulsar_utils__.click('#sp-hot-sale a')",
        "__pulsar_utils__.click('#choose-service a')",
        "__pulsar_utils__.click('#choose-baitiao a')",
        "__pulsar_utils__.click('#detail li:nth-child(2)')",
        "__pulsar_utils__.click('#detail li:nth-child(3)')",
        "__pulsar_utils__.click('#detail li:nth-child(4)')",
        "__pulsar_utils__.click('#detail li:nth-child(5)')",
    ).shuffled().take(3)
}
