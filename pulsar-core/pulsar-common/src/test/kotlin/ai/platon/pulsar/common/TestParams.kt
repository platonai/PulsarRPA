package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.Params
import java.time.Duration
import kotlin.test.*

class TestParams {

    @Test
    fun testFormat() {
        val params = Params.of(
                "-expires", Duration.ofSeconds(1),
                "-storeContent", false,
                "-retryFailed", true,
                "-incognito", true,
        )

        val objectStyleLine = params.formatAsLine()
        assertTrue { "-storeContent: false" in objectStyleLine }

        val distinctBooleanParams = listOf("-storeContent")
        val cmdStyleLine = params.distinct().sorted()
                .withCmdLineStyle(true)
                .withKVDelimiter(" ")
                .withDistinctBooleanParams(distinctBooleanParams)
                .formatAsLine().replace("\\s+".toRegex(), " ")
        assertTrue(cmdStyleLine) { "-storeContent false" in cmdStyleLine }
    }
}
