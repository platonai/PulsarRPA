package ai.platon.pulsar.rest

import ai.platon.pulsar.persist.rdb.model.ServerInstance
import org.junit.Ignore
import org.junit.Test
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestServiceResource : ResourceTestBase() {

    @Test
    fun testEcho() {
        var serverInstance = ServerInstance("127.0.0.1", 19888, ServerInstance.Type.FetchService)
        serverInstance = target("service").path("echo").request(MediaType.APPLICATION_JSON)
                .post(Entity.json(serverInstance), ServerInstance::class.java)
        assertEquals(19888, serverInstance.port.toLong())
        assertEquals("127.0.0.1", serverInstance.ip)
    }

    @Ignore("NOTE: return of a list of integers is not supported by jersey-2.26-b03, use string instead")
    @Test
    fun testListOfInteger() {
        val listOfInteger = target("service").path("listOfInteger").request()
                .get(object : GenericType<List<Int>>() {

                })
        println(listOfInteger)
    }

    @Ignore("@context is not available if jersey-test-framework-provider-inmemory is used")
    @Test
    fun testRegister() {
        // javax.servlet.http.HttpServletRequest r = new MockMultipartHttpServletRequest();
        var serverInstance = ServerInstance("", 19888, ServerInstance.Type.FetchService)
        serverInstance = target("service").path("register").request(MediaType.APPLICATION_JSON)
                .post(Entity.json(serverInstance), ServerInstance::class.java)
        assertEquals(19888, serverInstance.port.toLong())
        assertEquals("127.0.0.1", serverInstance.ip)
    }

    @Ignore("Not available if jersey-test-framework-provider-inmemory is used")
    @Test
    fun testServiceResource() {
        // Register
        var serverInstance: ServerInstance? = null
        for (i in 0..0) {
            serverInstance = target("service").path("register").request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(ServerInstance("127.0.0.$i", 19888 + i, ServerInstance.Type.FetchService)), ServerInstance::class.java)
        }

        // List
        var serverInstances = target("service").request(MediaType.APPLICATION_JSON)
                .get(object : GenericType<List<ServerInstance>>() {

                })
        // System.out.println(serverInstances);
        assertTrue(serverInstances.contains(serverInstance))

        // Unregister
        serverInstance = target("service").path("unregister").path(serverInstance!!.id.toString())
                .request(MediaType.APPLICATION_JSON).delete(ServerInstance::class.java)
        serverInstances = target("service").request(MediaType.APPLICATION_JSON)
                .get(object : GenericType<List<ServerInstance>>() {

                })
        // System.out.println(serverInstances);
        assertFalse(serverInstances.contains(serverInstance))
    }

    companion object {
        private val FetchServerName = ServerInstance.Type.FetchService.name
    }
}
