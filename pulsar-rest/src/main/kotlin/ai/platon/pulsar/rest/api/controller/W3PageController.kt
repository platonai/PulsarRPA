package ai.platon.scent.rest.api.controller

import ai.platon.scent.rest.api.serialize.W3DocumentRequest
import ai.platon.scent.rest.api.service.LoadService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * The controller to handle www resources
 * */
@RestController
@CrossOrigin
@RequestMapping("w3page")
class W3PageController {

    @Autowired
    lateinit var loadService: LoadService

    @GetMapping
    fun loadContent(@RequestParam url: String): String {
        return loadService.load(url).contentAsString
    }
}
