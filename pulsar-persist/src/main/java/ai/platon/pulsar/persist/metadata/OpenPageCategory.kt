package ai.platon.pulsar.persist.metadata

class OpenPageCategory(val name: String, val symbol: String) {
    constructor(pageCategory: PageCategory): this(pageCategory.name, pageCategory.symbol())
    fun toPageCategory() = PageCategory.parse(name)
}
