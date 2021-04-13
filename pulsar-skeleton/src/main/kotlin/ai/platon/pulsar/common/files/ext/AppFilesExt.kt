package ai.platon.pulsar.common.files.ext

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.AppPaths.WEB_CACHE_DIR
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import org.jsoup.nodes.Document
import java.nio.file.Files
import java.nio.file.Path

private val monthDay = DateTimes.now("MMdd")

fun AppFiles.export(status: ProtocolStatus, content: String, page: WebPage): Path {
    return AppFiles.export(StringBuilder(), status, content, page)
}

fun AppFiles.export(
    sb: StringBuilder,
    status: ProtocolStatus,
    content: String,
    page: WebPage,
    suffix: String = ".htm",
): Path {
    val document = Documents.parse(content, page.baseUrl)
    document.absoluteLinks()
    val prettyHtml = document.prettyHtml
    val length = prettyHtml.length

    sb.setLength(0)
    sb.append(status.minorName).append('/').append(monthDay)
    if (length < 2000) {
        sb.append("/a").append(length / 500 * 500)
    } else {
        sb.append("/b").append(length / 20000 * 20000)
    }

    val ident = sb.toString()
    val path = export(page, prettyHtml.toByteArray(), ident, suffix)

    page.metadata.set(Name.ORIGINAL_EXPORT_PATH, path.toString())

    return path
}

fun AppFiles.export(page: WebPage, content: ByteArray, ident: String = "", suffix: String = ".htm"): Path {
    val browser = page.lastBrowser.name.toLowerCase()

//    val u = Urls.getURLOrNull(page.url)?: return AppPaths.TMP_DIR
//    val domain = if (Strings.isIpPortLike(u.host)) u.host else InternetDomainName.from(u.host).topPrivateDomain().toString()
//    val filename = ident + "-" + DigestUtils.md5Hex(page.url) + suffix

    val filename = AppPaths.fromUri(page.url, suffix = suffix)
    val path = WEB_CACHE_DIR.resolve("original").resolve(browser).resolve("$ident-$filename")
    saveTo(content, path, true)

    return path
}

fun AppFiles.export(page: WebPage, ident: String = "", suffix: String = ""): Path {
    val filename = page.headers.decodedDispositionFilename ?: AppPaths.fromUri(page.location, "", suffix)
    val path = WEB_CACHE_DIR.resolve(ident).resolve(filename)
    Files.deleteIfExists(path)
    AppFiles.saveTo(page.content?.array() ?: "(empty)".toByteArray(), path)
    return path
}

fun AppFiles.export(doc: Document, ident: String = ""): Path {
    val path = WEB_CACHE_DIR.resolve(ident).resolve(AppPaths.fromUri(doc.baseUri(), "", ".htm"))
    return AppFiles.saveTo(doc.outerHtml(), path)
}
