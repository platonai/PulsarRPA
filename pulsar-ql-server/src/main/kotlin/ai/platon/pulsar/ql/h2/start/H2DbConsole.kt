package ai.platon.pulsar.ql.h2.start

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.setPropertyIfAbsent
import com.google.common.collect.Lists
import org.h2.tools.Console
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

/**
 * Server start port
 */
@SpringBootApplication(exclude = [MongoAutoConfiguration::class, EmbeddedMongoAutoConfiguration::class])
open class H2DbConsole {
    val log = LoggerFactory.getLogger(H2DbConsole::class.java)

    @Bean
    open fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner { args ->
            if (log.isInfoEnabled) {
                val beans = ctx.beanDefinitionNames.sorted()
                val s = Lists.partition(beans, 10).joinToString("\n\t") { it.joinToString { it } }
                log.info("Defined beans: {}", s)
            }
            Console().runTool(*args)
        }
    }

    companion object {
        private val env = PulsarEnv.getOrCreate()

        @JvmStatic
        fun main(args: Array<String>) {
            setPropertyIfAbsent("h2.sessionFactory", ai.platon.pulsar.ql.h2.H2SessionFactory::class.java.name)
            // setPropertyIfAbsent(CapabilityTypes.PROXY_ENABLE_INTERNAL_SERVER, "true")

            SpringApplication.run(H2DbConsole::class.java, *args)
        }
    }
}
