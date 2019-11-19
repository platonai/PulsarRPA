package ai.platon.pulsar.common.files.ext

import ai.platon.pulsar.common.PulsarFiles
import ai.platon.pulsar.common.PulsarPaths
import ai.platon.pulsar.common.PulsarPaths.FILE_CACHE_DIR
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document
import java.nio.file.Files
import java.nio.file.Path

fun PulsarFiles.save(page: WebPage, ident: String = ""): Path {
    val filename = page.headers.decodedDispositionFilename ?: PulsarPaths.fromUri(page.location)
    var postfix = filename.substringAfter(".").toLowerCase()
    if (postfix.length > 5) {
        postfix = "other"
    }
    val path = PulsarPaths.get(FILE_CACHE_DIR, ident, postfix, filename)
    if (!Files.exists(path)) {
        PulsarFiles.saveTo(page.content?.array()?: "(empty)".toByteArray(), path)
    }
    return path
}

fun PulsarFiles.save(doc: Document, ident: String = ""): Path {
    val path = PulsarPaths.get(FILE_CACHE_DIR, ident, PulsarPaths.fromUri(doc.baseUri(), ".htm"))
    return PulsarFiles.saveTo(doc.outerHtml(), path)
}
