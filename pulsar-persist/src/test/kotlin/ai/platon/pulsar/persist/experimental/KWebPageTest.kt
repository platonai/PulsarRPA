package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.persist.gora.generated.GWebPage
import kotlin.test.Test
import kotlin.test.assertFalse

class KWebPageTest {
    private val pageImpl = GoraWebAssetImpl(GWebPage.newBuilder().build())
    private val page = KWebPage(pageImpl)
    
    @Test
    fun `when create a new page then all states are false`() {
        assertFalse(page.isCached)
        assertFalse(page.isLoaded)
        assertFalse(page.isFetched)
        assertFalse(page.isCanceled)
        assertFalse(page.isContentUpdated)
    }
}
