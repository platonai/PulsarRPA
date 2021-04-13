package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.RandomStringUtils

class SQLInstance(
    val url: String,
    val template: SQLTemplate,
    val name: String = template.name,
) {
    val sql = template.createInstance(url)

    override fun toString() = sql

    companion object {
        private val generatedName: String = RandomStringUtils.randomAlphabetic(4)

        fun load(url: String, resource: String, name: String = generatedName): SQLInstance {
            val template = SQLTemplate.load(resource, name = name)
            return SQLInstance(url, template, name)
        }
    }
}
