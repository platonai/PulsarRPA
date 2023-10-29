package ai.platon.pulsar.persist

import ai.platon.pulsar.persist.gora.generated.GWebPage
import kotlin.test.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestWebPage {

    @Test
    fun testFieldEnums() {
        val field = GWebPage.Field.BATCH_ID
        assertNotEquals(field.toString(), field.name)
        assertEquals(field.toString(), field.getName())
    }
}
