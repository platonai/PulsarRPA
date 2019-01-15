package `fun`.platonic.pulsar.dom.model

import `fun`.platonic.pulsar.dom.model.Website
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

class WebsiteFactory {
    val resource = "conf/known-websites.xml"
    val learningFile = "output/learning/website.txt"

    // maintains all websites
    var websites = ArrayList<Website>()
    // an index to websites
    var domain2websites: MutableMap<String, Website> = HashMap()
    // maintains all learned websites
    var learnedWebsites = ArrayList<Website>()

    fun resource(): String {
        return resource
    }

    operator fun get(domain: String): Website? {
        return domain2websites[domain]
    }

    fun getName(domain: String): String {
        val w = domain2websites[domain]

        return w?.name ?: ""

    }

    private fun load(file: String) {
        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = db.parse(File(resource))

        parse(doc)

        rebuild()
    }

    private fun rebuild() {
        for (website in websites) {
            domain2websites[website.domain] = website
        }
    }

    private fun parse(doc: Document) {
        val rootNode = doc.firstChild
        val websiteNodes = rootNode.childNodes

        for (i in 0 until websiteNodes.length) {
            val websiteNode = websiteNodes.item(i)

            if (websiteNode.nodeType != Node.ELEMENT_NODE || websiteNode.nodeName != "website") {
                continue
            }

            val domain = websiteNode.attributes.getNamedItem("domain").nodeValue
            var name: String? = null

            val childNodes = websiteNode.childNodes
            for (j in 0 until childNodes.length) {
                val childNode = childNodes.item(j)

                if (childNode.nodeType != Node.ELEMENT_NODE || childNode.nodeName != "name") {
                    continue
                }

                name = childNode.textContent
            }

            if (StringUtils.isNotEmpty(domain) && StringUtils.isNotEmpty(name)) {
                websites.add(Website(domain, name!!))
            }
        }
    }
}
