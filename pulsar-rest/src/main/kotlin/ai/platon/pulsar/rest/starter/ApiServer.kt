package ai.platon.pulsar.rest.starter

import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import ai.platon.pulsar.rest.api.ApiApplication
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args) {
        addInitializers(PulsarContextInitializer())
        setAdditionalProfiles("rest")
        setLogStartupInfo(true)
    }
}
