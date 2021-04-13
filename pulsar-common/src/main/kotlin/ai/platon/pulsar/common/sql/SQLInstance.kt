package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.RandomStringUtils

class SQLInstance(
    val url: String,
    val template: SQLTemplate,
    val name: String = template.name,
) {
    val sql by lazy { template.createInstance(url) }

    override fun toString() = sql

    companion object {
        private val randomName: String = RandomStringUtils.randomAlphabetic(4)

        fun load(url: String, resource: String, name: String = randomName): SQLInstance {
            val template = SQLTemplate.load(resource, name = name)
            return SQLInstance(url, template, name)
        }
    }
}
