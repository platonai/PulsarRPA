package ai.platon.pulsar.browser.driver.ws.server

import ai.platon.pulsar.browser.driver.ws.server.plugins.configureRouting
import ai.platon.pulsar.browser.driver.ws.server.plugins.configureSockets
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureRouting()
    configureSockets()
}
