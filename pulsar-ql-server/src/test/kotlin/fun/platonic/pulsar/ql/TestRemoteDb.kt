package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.ql.Db.config
import `fun`.platonic.pulsar.ql.Db.getDBName
import org.junit.After
import org.junit.Before
import org.junit.Test

class TestRemoteDb : TestBase() {

    @Before
    override fun setup() {
        super.setup()

        config.traceTest = false
        config.memory = true
        config.networked = true
    }

    @After
    override fun teardown() {
        super.teardown()
        afterTest()
    }

    @Test
    fun testConnections() {
        for (i in 1..100) {
            val name = getDBName()
            val conn = Db.getConnection(name)
            val stat = conn.createStatement()
            execute("CALL ADMIN_ECHO('HELLO #${i}')")
        }
    }
}
