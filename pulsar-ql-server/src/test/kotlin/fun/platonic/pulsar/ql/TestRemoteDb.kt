package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.ql.Db.generateTempDbName
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore
class TestRemoteDb : TestBase() {

    @Before
    override fun setup() {
        Db.config.traceTest = true
        Db.config.memory = true
        Db.config.networked = true

        super.setup()
    }

    @After
    override fun teardown() {
        super.teardown()
        afterTest()
    }

    @Test
    fun testConnections() {
        for (i in 1..100) {
            val name = generateTempDbName()
            val conn = Db.getConnection(name)
            val stat = conn.createStatement()
            execute("CALL ADMIN_ECHO('HELLO #${i}')")
        }
    }
}
