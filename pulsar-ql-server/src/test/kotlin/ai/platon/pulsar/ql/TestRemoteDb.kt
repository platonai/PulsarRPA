package ai.platon.pulsar.ql

import org.junit.Ignore
import org.junit.Test

@Ignore
class TestRemoteDb : TestBase() {

    @Test
    fun testConnections() {
        for (i in 1..100) {
            val name = remoteDB.generateTempDbName()
            val conn = remoteDB.getConnection(name)
            val stat = conn.createStatement()
            execute("CALL ADMIN_ECHO('HELLO #${i}')")
        }
    }
}
