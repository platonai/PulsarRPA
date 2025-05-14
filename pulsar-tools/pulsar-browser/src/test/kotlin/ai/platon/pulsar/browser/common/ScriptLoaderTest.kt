package ai.platon.pulsar.browser.common

import ai.platon.pulsar.common.AppPaths
import org.junit.jupiter.api.BeforeEach
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class ScriptLoaderTest {
    private lateinit var scriptLoader: ScriptLoader

    @BeforeEach
    fun setUp() {
        val confuser = SimpleScriptConfuser()
        confuser.clear()
        scriptLoader = ScriptLoader(confuser, listOf("style", "color"))
    }

    @Test
    fun `test getPreloadJs`() {
        val js = scriptLoader.getPreloadJs(true)
        assert(js.isNotBlank())
        val path = AppPaths.getProcTmpDirectory("script.js")
        path.writeText(js)
        println("Js saved: ${path.toUri()}")
        assertTrue { js.contains("style") }
        assert(js.contains("__pulsar_utils__"))
    }
}