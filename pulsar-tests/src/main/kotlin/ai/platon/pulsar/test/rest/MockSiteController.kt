package ai.platon.pulsar.test.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MockSiteController {
    @GetMapping("hello")
    fun hello(): String {
        return "Hello, World!"
    }
    
    @GetMapping("text", produces = ["text/plain"])
    fun text(): String {
        return "Hello, World! This is a plain text."
    }
    
    @GetMapping("csv", produces = ["text/csv"])
    fun csv(): String {
        return """
1,2,3,4,5,6,7
a,b,c,d,e,f,g
1,2,3,4,5,6,7
a,b,c,d,e,f,g
""".trimIndent()
    }
    
    @GetMapping("json", produces = ["application/json"])
    fun json(): String {
        return """{"message": "Hello, World! This is a json."}"""
    }
}
