package ai.platon.pulsar.skeleton.crawl.fetch.driver

import com.google.gson.JsonObject

class JsonCommandDispatcher {
    suspend fun dispatch(command: JsonObject, driver: WebDriver) {
        when (command.get("command").asString) {
            "addInitScript" -> driver.addInitScript(command.get("script").asString)
            "addBlockedURLs" -> driver.addBlockedURLs(command.get("urlPatterns").asJsonArray.map { it.asString })
            "click" -> driver.click(command.get("selector").asString, command.get("count").asInt)
        }
    }
}
