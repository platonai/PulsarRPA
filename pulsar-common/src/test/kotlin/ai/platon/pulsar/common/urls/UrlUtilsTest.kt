package ai.platon.pulsar.common.urls

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UrlUtilsTest {
    @Test
    fun testNormalizer() {
        var url = "https://www.amazon.com/s?k=\"Boys%27+Novelty+Belt+Buckles\"&rh=n:9057119011&page=1"
        var normUrl = UrlUtils.normalizeOrNull(url, true)
        assertNull(normUrl)
        
        url = "https://www.amazon.com/s?k=Boys%27+Novelty+Belt+Buckles&rh=n:9057119011&page=1"
        normUrl = UrlUtils.normalizeOrNull(url, true)
        assertNotNull(normUrl)
    }
}
