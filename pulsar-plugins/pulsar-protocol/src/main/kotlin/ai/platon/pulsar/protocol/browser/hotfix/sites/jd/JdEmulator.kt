package ai.platon.pulsar.protocol.browser.hotfix.sites.jd

class JdEmulator {
    val expressions = listOf(
        "document.querySelector('#summary-service a').click()",
        "document.querySelector('#ns_services a').click()",
        "document.querySelector('.summary-price a').click()",
        "document.querySelector('#comment-count a').click()",
        "document.querySelector('#sp-hot-sale a').click()",
        "document.querySelector('#choose-service a').click()",
        "document.querySelector('#choose-baitiao a').click()",
        "document.querySelector('#detail li:nth-child(2)').click()",
        "document.querySelector('#detail li:nth-child(3)').click()",
        "document.querySelector('#detail li:nth-child(4)').click()",
        "document.querySelector('#detail li:nth-child(5)').click()",
    ).shuffled().take(3)
}
