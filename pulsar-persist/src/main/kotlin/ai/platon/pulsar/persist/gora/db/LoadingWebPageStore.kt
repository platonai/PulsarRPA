package ai.platon.pulsar.persist.gora.db

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.memory.store.MemStore
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

class LoadingWebPageStore(
        val persistDirectory: Path
): MemStore<String, GWebPage>() {

    override fun get(key: String): GWebPage? {
        var page = super.get(key)
        if (page == null) {
            page = read(key)
        }
        return page
    }

    override fun put(key: String, page: GWebPage) {
        super.put(key, page)
        write(WebPage.box(Urls.unreverseUrl(key), page))
    }

    private fun read(key: String): GWebPage? {
        val url = Urls.unreverseUrl(key)
        val filename = AppPaths.fromUri(url)
        val path = persistDirectory.resolve(filename)
        if (Files.exists(path)) {
            val content = Files.readAllBytes(path)
            val page = WebPage.newWebPage(url)
            page.content = ByteBuffer.wrap(content)
            return page.unbox()
        }
        return null
    }

    private fun write(page: WebPage) {
        val url = Urls.unreverseUrl(page.key)
        val filename = AppPaths.fromUri(url)
        val path = persistDirectory.resolve(filename)
        page.content?.let { Files.write(path, it.array()) }
    }
}
