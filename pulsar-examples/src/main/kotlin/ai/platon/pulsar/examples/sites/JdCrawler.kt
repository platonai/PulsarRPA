package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.examples.Crawler

val portalUrl = "https://item.jd.com/100006386682.html"
val args = "-ic -i 1d -ii 1s -tl 5 -ol \"a[href~=item]\""

fun main() = Crawler().use { it.loadOutPages(portalUrl, args) }
