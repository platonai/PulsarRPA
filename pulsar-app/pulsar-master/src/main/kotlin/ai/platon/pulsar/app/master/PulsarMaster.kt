package ai.platon.pulsar.app.master

import ai.platon.pulsar.app.PulsarApplication
import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<PulsarApplication>(*args) {
        addInitializers(PulsarContextInitializer())
        setAdditionalProfiles("master", "private")
        setLogStartupInfo(true)
    }
}
