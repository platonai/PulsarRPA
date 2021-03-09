package ai.platon.pulsar.app.hello

import ai.platon.pulsar.boot.autoconfigure.pulsar.PulsarContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
class Application {
    @Bean
    fun hello(): Int {
        println("hello world")
        return 0
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(Application::class.java)
        .initializers(PulsarContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
