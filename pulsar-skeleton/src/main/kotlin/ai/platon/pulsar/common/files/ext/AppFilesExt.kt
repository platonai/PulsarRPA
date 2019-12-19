package ai.platon.pulsar.common.files.ext

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.AppPaths.FILE_CACHE_DIR
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document
import java.nio.file.Files
import java.nio.file.Path

fun AppFiles.save(page: WebPage, ident: String = ""): Path {
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

fun AppFiles.save(doc: Document, ident: String = ""): Path {
    val path = AppPaths.get(FILE_CACHE_DIR, ident, AppPaths.fromUri(doc.baseUri(), ".htm"))
    return AppFiles.saveTo(doc.outerHtml(), path)
}
