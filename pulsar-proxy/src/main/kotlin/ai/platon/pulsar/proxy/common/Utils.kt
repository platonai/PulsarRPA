package ai.platon.pulsar.proxy.common

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.AsciiString
import java.io.Serializable
import java.nio.charset.Charset
import java.util.regex.Pattern

object ProtoUtil {

    fun getRequestProto(httpRequest: HttpRequest): RequestProto? {
        val requestProto = RequestProto()
        var port = -1
        var hostStr: String? = httpRequest.headers().get(HttpHeaderNames.HOST)
        if (hostStr == null) {
            val pattern = Pattern.compile("^(?:https?://)?(?<host>[^/]*)/?.*$")
            val matcher = pattern.matcher(httpRequest.uri())
            if (matcher.find()) {
                hostStr = matcher.group("host")
            } else {
                return null
            }
        }
        val uriStr = httpRequest.uri()
        val pattern = Pattern.compile("^(?:https?://)?(?<host>[^:]*)(?::(?<port>\\d+))?(/.*)?$")
        var matcher = pattern.matcher(hostStr!!)
        //先从host上取端口号没取到再从uri上取端口号 issues#4
        var portTemp: String? = null
        if (matcher.find()) {
            requestProto.host = matcher.group("host")
            portTemp = matcher.group("port")
            if (portTemp == null) {
                matcher = pattern.matcher(uriStr)
                if (matcher.find()) {
                    portTemp = matcher.group("port")
                }
            }
        }
        if (portTemp != null) {
            port = Integer.parseInt(portTemp)
        }
        val isSsl = uriStr.indexOf("https") == 0 || hostStr.indexOf("https") == 0
        if (port == -1) {
            if (isSsl) {
                port = 443
            } else {
                port = 80
            }
        }
        requestProto.port = port
        requestProto.ssl = isSsl
        return requestProto
    }

    class RequestProto : Serializable {
        var host: String? = null
        var port: Int = 0
        var ssl: Boolean = false

        constructor() {}

        constructor(host: String, port: Int, ssl: Boolean) {
            this.host = host
            this.port = port
            this.ssl = ssl
        }

        companion object {

            private const val serialVersionUID = -6471051659605127698L
        }
    }
}


object ByteUtil {


    fun findText(byteBuf: ByteBuf, str: String): Int {
        val text = str.toByteArray()
        var matchIndex = 0
        for (i in byteBuf.readerIndex() until byteBuf.readableBytes()) {
            for (j in matchIndex until text.size) {
                if (byteBuf.getByte(i) == text[j]) {
                    matchIndex = j + 1
                    if (matchIndex == text.size) {
                        return i
                    }
                } else {
                    matchIndex = 0
                }
                break
            }
        }
        return -1
    }

    fun insertText(byteBuf: ByteBuf, index: Int, str: String, charset: Charset = Charset.defaultCharset()): ByteBuf {
        val begin = ByteArray(index + 1)
        val end = ByteArray(byteBuf.readableBytes() - begin.size)
        byteBuf.readBytes(begin)
        byteBuf.readBytes(end)
        byteBuf.writeBytes(begin)
        byteBuf.writeBytes(str.toByteArray(charset))
        byteBuf.writeBytes(end)
        return byteBuf
    }
}


object HttpUtil {

    /**
     * 检测url是否匹配
     */
    fun checkUrl(httpRequest: HttpRequest, regex: String?): Boolean {
        val host = httpRequest.headers().get(HttpHeaderNames.HOST)
        if (host != null && regex != null) {
            val url: String
            if (httpRequest.uri().indexOf("/") == 0) {
                if (httpRequest.uri().length > 1) {
                    url = host + httpRequest.uri()
                } else {
                    url = host
                }
            } else {
                url = httpRequest.uri()
            }
            return url.matches(regex.toRegex())
        }
        return false
    }

    /**
     * 检测头中的值是否为预期
     *
     * @param httpHeaders
     * @param name
     * @param regex
     * @return
     */
    fun checkHeader(httpHeaders: HttpHeaders, name: AsciiString, regex: String): Boolean {
        val s = httpHeaders.get(name)
        return s != null && s.matches(regex.toRegex())
    }

    /**
     * 检测是否为请求网页资源
     */
    fun isHtml(httpRequest: HttpRequest, httpResponse: HttpResponse): Boolean {
        val accept = httpRequest.headers().get(HttpHeaderNames.ACCEPT)
        val contentType = httpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE)
        return httpResponse.status().code() == 200 && accept != null && accept
                .matches("^.*text/html.*$".toRegex()) && contentType != null && contentType
                .matches("^text/html.*$".toRegex())
    }
}
