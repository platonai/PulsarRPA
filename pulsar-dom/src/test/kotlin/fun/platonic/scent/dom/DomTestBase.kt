package `fun`.platonic.scent.dom

import `fun`.platonic.pulsar.Pulsar
import `fun`.platonic.pulsar.net.SeleniumEngine
import `fun`.platonic.scent.dom.data.BrowserControl
import `fun`.platonic.scent.dom.nodes.DescriptiveDocument
import org.junit.Before

open class DomTestBase {

    var url = "https://baike.baidu.com/item/柏拉图/85471"
    var doc: DescriptiveDocument
    val pulsar = Pulsar()

    init {
        SeleniumEngine.CLIENT_JS = BrowserControl(pulsar.immutableConfig).getJs()
        doc = DescriptiveDocument(pulsar.parse(pulsar.load(url)))
    }

    @Before
    fun setup() {
    }
}
