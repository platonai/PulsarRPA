package ai.platon.pulsar.app

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource

@SpringBootApplication(exclude = [MongoAutoConfiguration::class, EmbeddedMongoAutoConfiguration::class])
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan(basePackages = ["ai.platon.pulsar.rest"])
open class ApiServer

fun main(args: Array<String>) {
    SpringApplication.run(ApiServer::class.java, *args)
}
