package ai.platon.pulsar.ql

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class TestCases: TestBase() {

    @Test
    fun testXSqlHelp() {
        execute("SELECT * FROM XSQL_HELP()")
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

    @Test
    fun loadAndSelectNeeq() {
        val url = "http://www.neeq.com.cn/nq/listedcompany.html"
        // execute("select dom, dom_css_selector(dom), dom_text(dom), dom_text_length(dom) from dom_load_and_select('$url', 'tbody > tr');", remote = true)
        val sql = """
        select
            dom, 
            dom_css_selector(dom), 
            dom_nth_text(dom, 'td', 1) as code, 
            dom_nth_text(dom, 'td', 2) as short_name, 
            dom_nth_text(dom, 'td', 3) as category, 
            dom_nth_text(dom, 'td', 4) as industry, 
            dom_nth_text(dom, 'td', 5) as trader,
            dom_nth_text(dom, 'td', 6) as city,
            dom_nth_text(dom, 'td', 7) as links
        from load_and_select('$url', 'tbody:not(:first-child) > tr');
        """.trimIndent()
        execute(sql, remote = true)
    }

    @Test
    fun loadInlineSelectNeeq() {
        val cssQuery = "tbody:not(:first-child) > tr td:nth-child(7) > a:nth-child(2)"
        val sql = """
            select 
                dom_doc_title(dom), 
                dom_inline_select_text(dom, 'table tr'),
                dom_base_uri(dom)
            from
                load_out_pages('http://www.neeq.com.cn/nq/listedcompany.html', '$cssQuery', 1, 20)
        """.trimIndent()
        execute(sql, remote = true)
    }

    @Test
    fun loadOutPagesAndSelectNeeq() {
        val cssQuery = "tbody:not(:first-child) > tr td:nth-child(7) > a:nth-child(2)"
        val sql = """
            select
                dom_base_uri(tr),
                dom_first_text(tr, 'td:nth-child(1)') as code,
                dom_first_text(tr, 'td:nth-child(2)') as title,
                dom_first_text(tr, 'td:nth-child(3)') as date
            from
                (select dom as tr from load_out_pages_and_select('http://www.neeq.com.cn/nq/listedcompany.html', '$cssQuery', 1, 20, 'table tr'))
        """.trimIndent()

        execute(sql, remote = true)
    }
}
