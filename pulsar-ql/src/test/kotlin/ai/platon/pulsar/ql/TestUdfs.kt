package ai.platon.pulsar.ql

import org.junit.Ignore
import org.junit.Test

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestUdfs: TestBase() {

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
