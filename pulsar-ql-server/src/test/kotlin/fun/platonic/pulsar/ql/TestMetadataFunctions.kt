package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.PulsarSession
import org.junit.Test

class
estMetadataFunctions : TestBase() {

    @Test
    fun testMetadata() {
        execute("SELECT * FROM METAT_PARSE('${urlGroups["mia"]!![2]}')")

        val pulsar = PulsarSession()
        val page = pulsar.load("https://www.mia.com/item-1667324.html")
        val doc = pulsar.parse(page)
        println(doc)
    }
}
