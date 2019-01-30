package `fun`.platonic.pulsar.common.files.ext

import `fun`.platonic.pulsar.common.PulsarFiles
import `fun`.platonic.pulsar.common.PulsarPaths
import `fun`.platonic.pulsar.common.PulsarPaths.fileCacheDir
import `fun`.platonic.pulsar.persist.WebPage
import org.jsoup.nodes.Document
import java.nio.file.Files
import java.nio.file.Path

/**
 * Not good to extend PulsarFiles using PulsarFiles.method style, IDE do not friendly support it currently.
 * */
object PulsarFilesExt {
    fun save(page: WebPage, ident: String = ""): Path {
        val filename = page.headers.decodedDispositionFilename ?: PulsarPaths.fromUri(page.baseUrl)
        var postfix = filename.substringAfter(".").toLowerCase()
        if (postfix.length > 5) {
            postfix = "other"
        }
        val path = PulsarPaths.get(fileCacheDir, ident, postfix, filename)
        return PulsarFiles.saveTo(page, path)
    }

    fun saveTo(page: WebPage, path: Path): Path {
        if (!Files.exists(path)) {
            PulsarFiles.saveTo(page.content?.array()
                    ?: "(empty)".toByteArray(), path)
        }
        return path
    }

    fun save(doc: Document, ident: String = ""): Path {
        val path = PulsarPaths.get(fileCacheDir, ident, PulsarPaths.fromUri(doc.baseUri(), ".htm"))
        return PulsarFiles.saveTo(doc.outerHtml(), path)
    }
}
