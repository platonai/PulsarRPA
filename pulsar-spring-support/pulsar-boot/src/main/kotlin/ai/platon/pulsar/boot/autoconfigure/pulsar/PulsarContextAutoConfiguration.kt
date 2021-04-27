package ai.platon.pulsar.boot.autoconfigure.pulsar

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.ql.context.SQLContexts
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class PulsarContextAutoConfiguration(
    val applicationContext: ApplicationContext,
) {
    @Bean
    @Scope("prototype")
    fun getPulsarSession(): PulsarSession {
        return SQLContexts.activate(applicationContext).createSession()
    }
}
