package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.PulsarContext.unmodifiedConfig
import ai.platon.pulsar.common.PulsarFiles
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2QueryEngine
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context
import org.slf4j.LoggerFactory

@UDFGroup(namespace = "ADMIN")
object AdminFunctions {
    val LOG = LoggerFactory.getLogger(AdminFunctions::class.java)

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
        val path = PulsarPaths.get(PulsarPaths.webCacheDir.toString(), PulsarPaths.fromUri(page.url, ".htm"))
        return PulsarFiles.saveTo(page, path).toString()
    }

    @UDFunction
    @JvmStatic
    fun testProxy(ipPort: String): String {
        val proxyPool = ai.platon.pulsar.common.proxy.ProxyPool.getInstance(unmodifiedConfig)
        return proxyPool.toString()
    }

    private fun checkPrivilege(h2session: Session) {

    }
}
