package ai.platon.scent.rest.api.controller

import ai.platon.pulsar.rest.api.entities.W3DocumentRequest
import ai.platon.pulsar.rest.api.service.LoadService
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
@RequestMapping("w3doc")
class W3DocController {

    @Autowired
    lateinit var loadService: LoadService

    @GetMapping
    fun get(request: W3DocumentRequest): String {
        val document = loadService.loadDocument(request.url, request.args)
        return document.prettyHtml
    }
}
