package ai.platon.pulsar.ql.h2.starter

import ai.platon.pulsar.PulsarEnv
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
import org.springframework.context.annotation.ImportResource

/**
 * Server start port
 */
@SpringBootApplication(exclude = [MongoAutoConfiguration::class, EmbeddedMongoAutoConfiguration::class])
@ImportResource("classpath:pulsar-beans/app-context.xml")
class H2DbConsole {
    val log = LoggerFactory.getLogger(H2DbConsole::class.java)
    private val env = PulsarEnv.getOrCreate()

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
}

fun main(args: Array<String>) {
    setPropertyIfAbsent("h2.sessionFactory", ai.platon.pulsar.ql.h2.H2SessionFactory::class.java.name)
    SpringApplication.run(H2DbConsole::class.java, *args)
}
