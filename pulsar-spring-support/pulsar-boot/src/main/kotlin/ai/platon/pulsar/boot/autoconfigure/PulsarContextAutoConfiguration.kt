package ai.platon.pulsar.boot.autoconfigure

import ai.platon.pulsar.ql.context.H2SQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.session.PulsarSession
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
        val context = SQLContexts.create(applicationContext)
        require(context is H2SQLContext)
        require(context.applicationContext == applicationContext)
        return context.createSession()
    }
}
