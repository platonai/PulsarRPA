package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import com.github.kklisura.cdt.protocol.v2023.events.network.ResponseReceivedExtraInfo
import com.github.kklisura.cdt.protocol.v2023.types.network.Response
import java.net.InetAddress
import java.nio.ByteBuffer

class CDPResponse(
        val driver: ChromeDevtoolsDriver,
        val request: CDPRequest,
        val response: Response,
        val extraInfo: ResponseReceivedExtraInfo? = null
) {
    var remoteAddress: InetAddress? = null
    var statusCode: Int = 0
    var statusText: String = ""
    var url: String? = null
    var fromDiskCache: Boolean = false
    var fromServiceWorker: Boolean = false
    var headers: MutableMap<String, String> = mutableMapOf()
    var content: ByteBuffer? = null
    
    fun resolveBody(body: String?) {
        TODO()
    }
}
