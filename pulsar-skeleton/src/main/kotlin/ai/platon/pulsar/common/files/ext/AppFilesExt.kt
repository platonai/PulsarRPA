package ai.platon.pulsar.common.files.ext

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.AppPaths.FILE_CACHE_DIR
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
import org.jsoup.nodes.Document
import java.nio.file.Files
import java.nio.file.Path

private val monthDay = DateTimeUtil.now("MMdd")

fun AppFiles.export(status: ProtocolStatus, content: String, page: WebPage): Path {
    return AppFiles.export(StringBuilder(), status, content, page)
}

fun AppFiles.export(sb: StringBuilder, status: ProtocolStatus, content: String, page: WebPage): Path {
    val document = Documents.parse(content, page.baseUrl)
    document.absoluteLinks()
    val prettyHtml = document.prettyHtml

    sb.setLength(0)
    sb.append(status.minorName).append('/').append(monthDay)
    if (prettyHtml.length < 2000) {
        sb.append("/a").append(prettyHtml.length / 500 * 500)
    } else {
        sb.append("/b").append(prettyHtml.length / 20000 * 20000)
    }

    val ident = sb.toString()
    val path = export(page, prettyHtml.toByteArray(), ident)

    page.metadata.set(Name.ORIGINAL_EXPORT_PATH, path.toString())

    return path
}

fun AppFiles.export(page: WebPage, content: ByteArray, ident: String = "", suffix: String = ".htm"): Path {
    val browser = page.lastBrowser.name.toLowerCase()

    val u = Urls.getURLOrNull(page.url)?: return AppPaths.TMP_DIR
    val domain = if (StringUtil.isIpPortLike(u.host)) u.host else InternetDomainName.from(u.host).topPrivateDomain().toString()
    val filename = ident + "-" + DigestUtils.md5Hex(page.url) + suffix
    val path = AppPaths.get(AppPaths.WEB_CACHE_DIR.toString(), "original", browser, domain, filename)
    AppFiles.saveTo(content, path, true)

    return path
}

fun AppFiles.export(page: WebPage, ident: String = ""): Path {
    val filename = page.headers.decodedDispositionFilename ?: AppPaths.fromUri(page.location)
    var postfix = filename.substringAfter(".").toLowerCase()
    if (postfix.length > 5) {
        postfix = "other"
    }
    val path = AppPaths.get(FILE_CACHE_DIR, ident, postfix, filename)
    if (!Files.exists(path)) {
        AppFiles.saveTo(page.content?.array()?: "(empty)".toByteArray(), path)
    }
    return path
}

fun AppFiles.export(doc: Document, ident: String = ""): Path {
    val path = AppPaths.get(FILE_CACHE_DIR, ident, AppPaths.fromUri(doc.baseUri(), ".htm"))
    return AppFiles.saveTo(doc.outerHtml(), path)
}
