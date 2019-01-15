package `fun`.platonic.pulsar.ql.h2.udfs

import `fun`.platonic.pulsar.common.PulsarContext.unmodifiedConfig
import `fun`.platonic.pulsar.common.PulsarFiles
import `fun`.platonic.pulsar.common.proxy.ProxyPool
import `fun`.platonic.pulsar.ql.annotation.UDFGroup
import `fun`.platonic.pulsar.ql.annotation.UDFunction
import `fun`.platonic.pulsar.ql.h2.H2QueryEngine
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context
import org.slf4j.LoggerFactory

@UDFGroup(namespace = "ADMIN")
object AdminFunctions {
    val LOG = LoggerFactory.getLogger(AdminFunctions::class.java)

    val fs = PulsarFiles()

    @UDFunction(deterministic = true) @JvmStatic
    fun echo(@H2Context h2session: Session, message: String): String {
        return message
    }

    @UDFunction(deterministic = true) @JvmStatic
    fun echo(@H2Context h2session: Session, message: String, message2: String): String {
        return "$message, $message2"
    }

    @UDFunction
    @JvmStatic
    fun print(message: String) {
        println(message)
    }

    @UDFunction
    @JvmStatic
    fun closeSession(@H2Context h2session: Session): String {
        checkPrivilege(h2session)

        LOG.info("About to close h2session {}", h2session)
        h2session.close()
        return h2session.toString()
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun save(@H2Context h2session: Session, url: String, postfix: String = ".htm"): String {
        checkPrivilege(h2session)
        val page = H2QueryEngine.getSession(h2session).load(url)
        val path = fs.paths.get("cache", "web", fs.fromUri(page.url, ".htm"))
        return fs.saveTo(page, path).toString()
    }

    @UDFunction
    @JvmStatic
    fun testProxy(ipPort: String): String {
        val proxyPool = ProxyPool.getInstance(unmodifiedConfig)
        return proxyPool.toString()
    }

    private fun checkPrivilege(h2session: Session) {

    }
}
