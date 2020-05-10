package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.examples.common.Crawler

fun main() = Crawler().use {
    it.loadOutPages("https://list.jd.com/list.html?cat=652,12345,12349", "-i 1s -ii 1s -ol a[href~=item]")
}
