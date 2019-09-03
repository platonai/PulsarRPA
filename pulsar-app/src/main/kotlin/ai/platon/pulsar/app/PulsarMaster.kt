package ai.platon.pulsar.app

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource

@SpringBootApplication(exclude = [MongoAutoConfiguration::class, EmbeddedMongoAutoConfiguration::class])
@ImportResource("classpath:pulsar-beans/app-context.xml")
open class PulsarMaster

fun main(args: Array<String>) {
    SpringApplication.run(PulsarMaster::class.java, *args)
}
