package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.RandomStringUtils

class SQLTemplate(
    val template: String,
    val resource: String? = null,
    val name: String = generatedName,
    var display: String = generateDisplay(resource, template)
) {
    fun createSQL(url: String) = createInstance(url).sql

    fun createInstance(url: String) = SQLInstance(url, this, name)

    override fun toString() = template

    companion object {
        private val generatedName: String = RandomStringUtils.randomAlphabetic(4)

        private fun generateDisplay(resource: String?, template: String): String {
            return resource?.substringAfterLast("/") ?: generatedName
        }

        fun load(resource: String, name: String = generatedName): SQLTemplate {
            return SQLTemplate(SQLUtils.loadSQL(resource), resource = resource, name = name)
        }
    }
}
