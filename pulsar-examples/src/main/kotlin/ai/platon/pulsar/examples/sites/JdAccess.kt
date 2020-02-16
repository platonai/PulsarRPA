package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.examples.WebAccess

fun main() {
    val portalUrl = "https://item.jd.com/100006386682.html"
    val args = """
        -ic -i 1d -ii 1s -tl 5 -ol "a[href~=item]"
    """.trimIndent()
    WebAccess().use { it.loadOutPages(portalUrl, args) }
}
