package ai.platon.pulsar.ql

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class TestCases: TestBase() {

    companion object {
        @BeforeClass
        fun setupClass() {
        }

        @AfterClass
        fun teardownClass() {
        }
    }

    @Test
    fun projectFields() {
        execute("SELECT DOM_TEXT(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '.welcome')")
        execute("SELECT DOM_TEXT(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '.nfPrice', 0, 5)")
        execute("SELECT DOM_SRC(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '.nfPic img', 0, 5)")

        execute("SELECT DOM_TITLE(DOM), DOM_ABS_HREF(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '.nfPic a', 0, 5)")
        execute("SELECT DOM_TITLE(DOM), DOM_ABS_HREF(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), 'a[href~=item]', 0, 5)")
    }

    @Test
    fun extractByCssBox() {
        execute("SELECT * FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '*:in-box(*,*,323,31)')") // TODO: failed
        execute("SELECT * FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '*:in-box(*,*,229,36)', 0, 5)")

        execute("SELECT IN_BOX_FIRST_TEXT(DOM_LOAD('$productIndexUrl'), '229x36')")
    }
}
