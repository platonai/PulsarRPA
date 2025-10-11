package ai.platon.pulsar.examples.sites.topEc.english.amazon

import ai.platon.pulsar.skeleton.context.PulsarContexts

fun main() = PulsarContexts.createSession().scrape("https://www.amazon.com/dp/B08PP5MSVB", "-expires 1s",
    mapOf("title" to "#title", "ratings" to "#acrCustomerReviewText")
).let { println(it) }
