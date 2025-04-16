package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.ql.TestBase
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class TestUdfs: TestBase() {

    @Ignore("Site unavailable")
    @Test
    fun testFirstFloat() {
        val sql = """
select
    dom_first_text(dom, '.c-goods-item__name') as Name,
    dom_first_float(dom, '.c-goods-item__sale-price', 0.0) as Price,
    dom_first_float(dom, '.c-goods-item__market-price', 0.0) as Tag_Price,
    dom_base_uri(dom)
from
    load_and_select('https://list.vip.com/autolist.html?rule_id=57889442 -i 1d', 'div.c-goods-item');
        """.trimIndent()

        execute(sql)
    }

    @Ignore("Transpose is not correctly implemented")
    @Test
    fun testTranspose() {
        execute("""
            select select make_array('C1R1', 'C1R2'), make_array('C2R1', 'C2R2')
        """.trimIndent())

        execute("""
            SELECT * FROM transpose(select make_array('C1R1', 'C1R2') as C1, make_array('C2R1', 'C2R2') as C2)
        """.trimIndent())
    }
}
