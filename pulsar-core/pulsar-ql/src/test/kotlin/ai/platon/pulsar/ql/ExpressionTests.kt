package ai.platon.pulsar.ql

import kotlin.test.Test

class ExpressionTests: TestBase() {
    @Test
    fun testEval() {
        val expr = "child > 20 && char > 100 && width > 800"
        execute("""SELECT DOM FROM LOAD_AND_GET_FEATURES('$productIndexUrl', '*:expr($expr)')""")
    }
}
