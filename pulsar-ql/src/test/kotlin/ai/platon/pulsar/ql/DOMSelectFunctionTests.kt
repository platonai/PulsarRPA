package ai.platon.pulsar.ql

import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class DOMSelectFunctionTests : TestBase() {
    private val url = "https://www.amazon.com/dp/B09V3KXJPB"

    @Before
    fun ensurePageIsGood() {
        val page = session.load("$url -requireSize 10000")
        require(page.protocolStatus.isSuccess)
    }

    @Test
    fun testSelectAllMultiAttributes() {
        val rs = query("""
select
    dom_all_attrs(dom, 'input[type=hidden]', 'name') as `hidden_input_names`,
    dom_all_attrs(dom, 'input[type=hidden]', 'value') as `hidden_input_values`,
    dom_all_multi_attrs(dom, 'input[type=hidden]', make_array('name', 'value')) as `hidden_input_properties`
from load_and_select('$url', 'body');
        """.trimIndent(), asList = true)

        assertTrue { rs.next() }
    }

    @Test
    fun testSelectFirstMultiAttributes() {
        val rs = query("""
select
    dom_first_attr(dom, 'input[type=hidden]', 'name') as `hidden_input_name`,
    dom_first_attr(dom, 'input[type=hidden]', 'value') as `hidden_input_value`,
    dom_first_multi_attrs(dom, 'input[type=hidden]', make_array('name', 'value')) as `hidden_input_property`
from load_and_select('$url', 'body');
        """.trimIndent(), asList = true)

        assertTrue { rs.next() }
    }
}
