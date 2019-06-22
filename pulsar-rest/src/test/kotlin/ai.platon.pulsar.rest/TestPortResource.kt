package ai.platon.pulsar.rest

import ai.platon.pulsar.persist.rdb.model.ServerInstance
import ai.platon.pulsar.rest.service.PortManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.glassfish.jersey.test.JerseyTest
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.util.ArrayList
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Application
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import kotlin.test.assertTrue

class TestPortResource : ResourceTestBase() {

    @Test
    fun testPortAcquire() {
        for (i in 0..49) {
            val port = target("port").path("acquire").queryParam("type", FetchServerName).request().get(Int::class.java)
            Assert.assertEquals((21001 + i).toLong(), port.toLong())
        }
    }

    @Test
    fun testGetManager() {
        val portManager = target("port").path("get-empty-port-manager")
                .queryParam("type", FetchServerName).request().get(PortManager::class.java)
        println(portManager)
    }

    @Test
    @Ignore("Failed to return a list of integer, we do not it's caused by a jersey bug or a dependency issue")
    fun testListOfInteger() {
        val listOfInteger = target("port").path("listOfInteger")
                .request(MediaType.APPLICATION_JSON).get(object : GenericType<ArrayList<Int>>() {

                })
        println(listOfInteger)
    }

    @Test
    fun testPortRecycle() {
        for (i in 0..49) {
            val port = target("port").path("acquire").queryParam("type", FetchServerName).request().get(Int::class.java)
            Assert.assertEquals((21001 + i).toLong(), port.toLong())
        }

        val port = target("port").path("acquire").queryParam("type", FetchServerName).request().get(Int::class.java)
        assertTrue(port > 21000)
        target("port").path("recycle").queryParam("type", FetchServerName)
                .request().put(Entity.entity(port, MediaType.TEXT_PLAIN_TYPE))

        //    List<Integer> freePorts = target("port").path("free").queryParam("type", FetchServerName)
        //        .request(MediaType.APPLICATION_JSON).get(new GenericType<List<Integer>>() {});
        val listType = object : TypeToken<ArrayList<Int>>() {

        }.type
        val result = target("port").path("legacy").path("free").queryParam("type", FetchServerName)
                .request(MediaType.TEXT_PLAIN).get(String::class.java)
        val freePorts = GsonBuilder().create().fromJson<List<Int>>(result, listType)

        //    System.out.println(port);
        //    System.out.println(freePorts);
        assertTrue(freePorts.contains(port))
    }

    companion object {
        private val FetchServerName = ServerInstance.Type.FetchService.name
    }
}
