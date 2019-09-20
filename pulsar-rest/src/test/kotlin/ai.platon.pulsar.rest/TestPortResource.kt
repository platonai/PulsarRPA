package ai.platon.pulsar.rest

import ai.platon.pulsar.persist.rdb.model.ServerInstance
import ai.platon.pulsar.rest.api.service.PortManager
import org.junit.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPortResource: ResourceTestBase() {

    @Test
    fun testPortAcquire() {
        for (i in 0..49) {
            val port = restTemplate.getForObject("$baseUri/port/acquire", Int::class.java, "type", FetchServerName)
            assertEquals((21001 + i), port)
        }
    }

    @Test
    fun testGetManager() {
        val portManager = restTemplate.getForObject("$baseUri/port/get-empty-port-manager", PortManager::class.java, "type", FetchServerName)
        println(portManager)
    }

    @Test
    fun testListOfInteger() {
        val restTemplate = RestTemplate()
        val response = restTemplate.exchange(
                "$baseUri/port/listOfInteger",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<Int>>() {},
                "type", FetchServerName
        )

        println(response.body)
    }

    @Test
    fun testPortRecycle() {
        for (i in 0..49) {
            val port = restTemplate.getForObject("$baseUri/port/acquire", Int::class.java, "type", FetchServerName)?:0
            assertEquals((21001 + i), port)
        }

        val port = restTemplate.getForObject("$baseUri/port/acquire", Int::class.java, "type", FetchServerName)?:0
        assertTrue(port > 21000)
        restTemplate.getForObject("$baseUri/port/acquire", Int::class.java, "type", FetchServerName)
        restTemplate.postForObject("$baseUri/port/recycle", Int::class.java, String::class.java, "type", FetchServerName)
    }

    companion object {
        private val FetchServerName = ServerInstance.Type.FetchService.name
    }
}
