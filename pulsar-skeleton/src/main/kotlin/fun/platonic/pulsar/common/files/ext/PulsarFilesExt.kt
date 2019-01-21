package `fun`.platonic.pulsar.common.files.ext

import `fun`.platonic.pulsar.common.PulsarFiles
import `fun`.platonic.pulsar.common.PulsarPaths
import `fun`.platonic.pulsar.persist.WebPage
import org.jsoup.nodes.Document
import java.nio.file.Files
import java.nio.file.Path

fun PulsarFiles.save(page: WebPage, ident: String = ""): Path {
    val filename = page.headers.decodedDispositionFilename ?: PulsarPaths.fromUri(page.baseUrl)
    var postfix = filename.substringAfter(".").toLowerCase()
    if (postfix.length > 5) {
        postfix = "other"
    }
    val path = PulsarPaths.get("cache", "files", ident, postfix, filename)
    return saveTo(page, path)
}

fun PulsarFiles.saveTo(page: WebPage, path: Path): Path {
    if (!Files.exists(path)) {
        PulsarFiles.saveTo(page.content?.array()
                ?: "(empty)".toByteArray(), path)
    }
    return path
}

fun PulsarFiles.save(doc: Document, ident: String = ""): Path {
    val path = PulsarPaths.get("cache", "html", ident, PulsarPaths.fromUri(doc.baseUri(), ".htm"))
    return PulsarFiles.saveTo(doc.outerHtml(), path)
}
