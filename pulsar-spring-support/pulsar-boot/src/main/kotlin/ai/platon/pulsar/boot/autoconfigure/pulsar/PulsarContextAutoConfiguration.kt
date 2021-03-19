package ai.platon.pulsar.boot.autoconfigure.pulsar

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.context.PulsarContexts
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.core.env.Environment

@Configuration
class PulsarContextAutoConfiguration(
    val applicationContext: ApplicationContext
) {
    @Bean
    @Scope("prototype")
    fun getPulsarSession(): PulsarSession {
        return PulsarContexts.activate(applicationContext).createSession()
    }
}
