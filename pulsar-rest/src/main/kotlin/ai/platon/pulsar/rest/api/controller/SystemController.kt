package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.skeleton.crawl.fetch.privacy.AbstractPrivacyManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * The controller to handle www resources
 * */
@RestController
@CrossOrigin
@RequestMapping("api/system")
class SystemController {

    @Autowired
    lateinit var driverPoolManager: WebDriverPoolManager

    @Autowired
    lateinit var privacyManager: AbstractPrivacyManager

    @GetMapping("health")
    fun health(): Map<String, String> {
        return mapOf("status" to "UP")
    }

    @GetMapping("hello")
    fun hello(): String {
        return "hello"
    }

    @GetMapping("report")
    fun report(): String {
        val sb = StringBuilder()
        sb.appendLine("Pulsar System Report")
        sb.appendLine(driverPoolManager.buildStatusString(true))
        sb.appendLine().appendLine()
        sb.appendLine(privacyManager.buildStatusString())
        return sb.toString()
    }
}
