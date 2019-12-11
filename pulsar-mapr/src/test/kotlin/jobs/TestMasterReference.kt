package jobs

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.jobs.fetch.service.jersey1.MasterReference
import ai.platon.pulsar.jobs.fetch.service.jersey1.ServerInstance
import com.beust.jcommander.internal.Lists
import com.sun.jersey.api.client.filter.LoggingFilter
import org.apache.commons.collections4.CollectionUtils
import org.junit.Assert
import org.junit.Test

/**
 * Created by vincent on 17-5-2.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestMasterReference {
    private val masterReference: MasterReference
    /**
     * Check PMaster is available.
     * NOTICE : MasterReference is compatible with PMaster in class api level, but is compatible in REST api level
     */
    fun checkPMaster(): Boolean {
        if (!masterReference.test()) {
            println("PMaster is not available")
            return false
        }
        return true
    }

    @Test
    fun testPortResource() {
        if (!checkPMaster()) {
            return
        }
        val type = ServerInstance.Type.FetchService
        val ports = Lists.newArrayList<Int>()
        for (i in 0..19) {
            val port = masterReference.acquirePort(type)
            // assertEquals(21000 + i, port);
            ports.add(port)
        }
        val freePorts = Lists.newArrayList<Int?>()
        for (i in 19 downTo 10) {
            val port = ports[i]
            freePorts.add(port)
            masterReference.recyclePort(type, port)
            Assert.assertTrue(port > 21000)
        }
        val freePorts2: List<Int?>? = masterReference.getFreePorts(type)
        Assert.assertTrue(CollectionUtils.containsAll(freePorts2, freePorts))
        //    System.out.println(freePorts);
//    System.out.println(freePorts2);
    }

    @Test
    fun testServerInsanceResourceEcho() {
        if (!checkPMaster()) {
            return
        }
        var serverInstance = ServerInstance("126.1.1.7", 21000, ServerInstance.Type.FetchService.name)
        serverInstance = masterReference.echo(serverInstance)
        Assert.assertEquals(21000, serverInstance.port.toLong())
        Assert.assertEquals("126.1.1.7", serverInstance.ip)
    }

    @Test
    fun testRegisterServerInsance() {
        if (!checkPMaster()) {
            return
        }
        for (i in 0..9) {
            val port = 21000 + i
            var serverInstance = ServerInstance("", port, ServerInstance.Type.FetchService.name)
            serverInstance = masterReference.register(serverInstance)
            // System.out.println(serverInstance);
            Assert.assertEquals(port.toLong(), serverInstance.port.toLong())
            //      assertEquals("127.0.0.1", serverInstance.getIp());
            serverInstance = masterReference.unregister(serverInstance.id)
            Assert.assertEquals(port.toLong(), serverInstance.port.toLong())
            //      assertEquals("127.0.0.1", serverInstance.getIp());
            serverInstance = masterReference.unregister(100000)
        }
    }

    init {
        masterReference = MasterReference(ImmutableConfig())
        masterReference.addFilter(LoggingFilter(System.out))
    }
}