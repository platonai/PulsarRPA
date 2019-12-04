package ai.platon.pulsar.app.master

import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.ql.h2.starter.H2DbConsole
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan("ai.platon.pulsar.rest.api", "ai.platon.pulsar.ql.h2.starter")
class PulsarMaster {

    @Bean
    fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner { args ->
            val beans = ctx.beanDefinitionNames.sorted()
            val s = beans.joinToString("\n") { it }
            val path = AppPaths.getTmp("spring-beans.txt")
            AppFiles.saveTo(s, path)
        }
    }
}

fun main(args: Array<String>) {
    val application = SpringApplication(PulsarMaster::class.java)

    val event = ApplicationListener<ApplicationEnvironmentPreparedEvent> {
        PulsarEnv.getOrCreate()
    }
    application.addListeners(event)

    application.run(*args)
}
