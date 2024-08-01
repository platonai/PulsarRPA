package ai.platon.pulsar.examples.sites.topEc.english.amazon

import ai.platon.pulsar.skeleton.context.PulsarContexts

fun main() = PulsarContexts.createSession().scrape("https://www.amazon.com/dp/B0C1H26C46", "-expires 1s",
    mapOf("title" to "#title", "ratings" to "#acrCustomerReviewText")
).let { println(it) }
