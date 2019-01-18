package `fun`.platonic.pulsar.ql

import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore
class TestRemoteDb : TestBase() {

    @Before
    override fun setup() {
        db.config.traceTest = true
        db.config.memory = true
        db.config.networked = true

        super.setup()
    }

    @Test
    fun testConnections() {
        for (i in 1..100) {
            val name = db.generateTempDbName()
            val conn = db.getConnection(name)
            val stat = conn.createStatement()
            execute("CALL ADMIN_ECHO('HELLO #${i}')")
        }
    }
}
