package ai.platon.pulsar.ql

import org.junit.Test

class TestCases: TestBase() {

    @Test
    fun testXSqlHelp() {
        execute("SELECT * FROM XSQL_HELP()")
    }

    @Test
    fun projectFields() {
        execute("SELECT 'welcome', DOM_TEXT(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '.welcome')", remote = true)
        execute("SELECT DOM_TEXT(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '.nfPrice', 0, 5)")
        execute("SELECT DOM_SRC(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '.nfPic img', 0, 5)")

        execute("SELECT DOM_TITLE(DOM), DOM_ABS_HREF(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), '.nfPic a', 0, 5)")
        execute("SELECT DOM_TITLE(DOM), DOM_ABS_HREF(DOM) FROM DOM_SELECT(DOM_LOAD('$productIndexUrl'), 'a[href~=item]', 0, 5)")
    }

    @Test
    fun projectFields2() {
        val sql = """
            SELECT
                DOM_HEIGHT(DOM), DOM_TEXT(DOM), DOM_BASE_URI(DOM), DOM_CSS_SELECTOR(DOM) 
            FROM
                DOM_SELECT(DOM_LOAD('https://www.mia.com/formulas.html'), '.welcome');
        """.trimIndent()
        execute(sql, remote = true)
    }

    @Test
    fun testCondition() {
        execute("SELECT * FROM LOAD_AND_GET_FEATURES('$productIndexUrl', '.nfItem');")
        execute("SELECT * FROM LOAD_AND_GET_FEATURES('$productIndexUrl', 'div', 1, 10000) " +
                "WHERE WIDTH BETWEEN 200 AND 400 AND HEIGHT BETWEEN 200 AND 400 AND TXT_ND > 0;")
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
        execute(sql, remote = false)
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

    @Test
    fun loadAndGetLinksCityflower() {
        val sql = """
select * 
from load_and_get_links('https://www.cityflower.net/attribute/21.html -i 1d', '.recommend a');
        """.trimIndent()

        execute(sql, remote = true)
    }

    @Test
    fun loadOutPagesAndSelectCityFlower() {
        val sql = """
select
    dom_text(dom)
from 
    load_out_pages_and_select('https://www.cityflower.net/attribute/21.html -i 1s', '.recommend a[href~=detail]', 1, 40, '.product_detail');
        """.trimIndent()

        execute(sql, remote = true)
    }

    @Test
    fun tokenizer() {
        execute("SELECT STR_CHINESE_TOKENIZE('目标公司为香港懋宏唯一股东')")
    }
}
