package ai.platon.pulsar.rest.starter

import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource

@ImportResource("classpath:rest-beans/app-context.xml")
@ComponentScan(basePackages = ["ai.platon.pulsar.rest.api"])
class ApiServer

fun main(args: Array<String>) {
    SpringApplication.run(ApiServer::class.java, *args)
}
