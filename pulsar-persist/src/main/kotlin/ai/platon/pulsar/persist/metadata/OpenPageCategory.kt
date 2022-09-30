package ai.platon.pulsar.persist.metadata

/**
 * Describe page categories.
 * */
class OpenPageCategory(
    /**
     * A descriptive name of this category.
     * */
    val name: String,
    /**
     * A short symbol for the category, with one or two characters recommended.
     * */
    val symbol: String
) {
    constructor(pageCategory: PageCategory): this(pageCategory.name, pageCategory.symbol())

    /**
     * Try to convert to a [PageCategory] enumeration, returns [PageCategory.UNKNOWN] if no such enumeration value.
     * */
    fun toPageCategory() = PageCategory.parse(name)

    /**
     * Format the category.
     * */
    fun format(): String {
        return "$name $symbol"
    }

    /**
     * Equals to the [name].
     * */
    override fun toString() = name

    companion object {

        /**
         * Parse a string to a [OpenPageCategory] object, [format] and this method are symmetric.
         * */
        fun parse(category: String): OpenPageCategory {
            val parts = category.split(" ")
            return when (parts.size) {
                1 -> OpenPageCategory(parts[0], "U")
                2 -> OpenPageCategory(parts[0], parts[1])
                else -> OpenPageCategory("", "")
            }
        }
    }
}
