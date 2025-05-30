package ai.platon.pulsar.test.server

import ai.platon.pulsar.common.ResourceLoader
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MockSiteController {
    @GetMapping("/")
    fun home(): String {
        return "Welcome! This site is used for internal test."
    }

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

    @GetMapping("robots.txt", produces = ["application/text"])
    fun robots(): String {
        return """
            User-agent: *
            Disallow: /exec/obidos/account-access-login
            Disallow: /exec/obidos/change-style
            Disallow: /exec/obidos/flex-sign-in
            Disallow: /exec/obidos/handle-buy-box
            Disallow: /exec/obidos/tg/cm/member/
            Disallow: /gp/aw/help/id=sss
            Disallow: /gp/cart
            Disallow: /gp/flex
            Disallow: /gp/product/e-mail-friend
            Disallow: /gp/product/product-availability
            Disallow: /gp/product/rate-this-item
            Disallow: /gp/sign-in
            Disallow: /gp/reader
            Disallow: /gp/sitbv3/reader
        """.trimIndent()
    }

    @GetMapping("amazon/home.htm", produces = ["text/html"])
    fun amazonHome(): String {
        return ResourceLoader.readString("pages/amazon/home.htm")
    }

    @GetMapping("amazon/product.htm", produces = ["text/html"])
    fun amazonProduct(): String {
        return ResourceLoader.readString("pages/amazon/B0C1H26C46.original.htm")
    }
}
