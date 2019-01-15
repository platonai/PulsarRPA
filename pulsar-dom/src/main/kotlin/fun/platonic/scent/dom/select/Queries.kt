package `fun`.platonic.scent.dom.select

/**
 * In-box syntax, cases:
 * <ul>
 *   <li>div:in-box(200, 200, 300, 300)</li>
 *   <li>div:in-box(200, 200, 300, 300, 50)</li>
 *   <li>*:in-box(200, 200, 300, 300)</li>
 *   <li>*:in-box(200, 200, 300, 300, 50)</li>
 *   <li>div:in-box(200, 200, 300, 300),*:in-box(200, 200, 300, 300, 50)</li>
 * </ul>
 *
 * Simplified in-box syntax version:
 * <ul>
 *   <li>200x300</li>
 *   <li>200x300,300x500</li>
 * </ul>
 * */
@JvmField val BOX_CSS_PATTERN_1 = Regex(".{1,5}:in-box\\(\\d+(,\\d+\\){1,4})")
@JvmField val BOX_CSS_PATTERN = Regex("$BOX_CSS_PATTERN_1(,$BOX_CSS_PATTERN_1)?")

@JvmField val BOX_SYNTAX_PATTERN_1 = Regex("(\\d+)[xX](\\d+)")
@JvmField val BOX_SYNTAX_PATTERN = Regex("$BOX_SYNTAX_PATTERN_1(\\s*,\\s*$BOX_SYNTAX_PATTERN_1)?")

/**
 * Convert a box query to a normal css query
 */
fun convertCssQuery(cssQuery: String): String {
    val query = cssQuery
    val matcher = BOX_SYNTAX_PATTERN.toPattern().matcher(query)
    if (matcher.find()) {
        return query.split(",")
                .map { it.split('x', 'X') }
                .joinToString { "*:in-box(${it[0]}, ${it[1]})" }
    }

    // Bad syntax, no element should find
    return cssQuery
}
