package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.AbstractPrivacyManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The controller to handle www resources
 * */
@RestController
@CrossOrigin
@RequestMapping("api/pulsar-system")
class PulsarSystemController {

    @Autowired
    lateinit var driverPoolManager: WebDriverPoolManager

    @Autowired
    lateinit var privacyManager: AbstractPrivacyManager

    @GetMapping("hello")
    fun hello(): String {
        return "hello"
    }

    @GetMapping("report")
    fun report(): String {
        val sb = StringBuilder()
        sb.appendLine("Pulsar system reporting")
        sb.appendLine(driverPoolManager.buildStatusString(true))
        sb.appendLine().appendLine()
        sb.appendLine(privacyManager.buildStatusString())
        return sb.toString()
    }
}
