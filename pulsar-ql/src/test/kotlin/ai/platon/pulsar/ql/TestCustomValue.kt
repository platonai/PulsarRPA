package ai.platon.pulsar.ql

import org.junit.jupiter.api.Test

class TestCustomValue: TestBase() {
    private val url = "https://www.example.com"

    @Test
    fun testDomValue() {
        query("SELECT DOM_TEXT(DOM) AS `book_categories` FROM LOAD_AND_SELECT('$productIndexUrl', '.bookclass_box')")
    }

    @Test
    fun testValueStringJSON() {
        val json = """
            {
                "a": 1,
                "b": 2,
                "c": 3
            }
        """.trimIndent()
        val javaClassName = Map::class.qualifiedName

        query("SELECT MAKE_VALUE_STRING_JSON()")
        query("SELECT MAKE_VALUE_STRING_JSON('$json', '$javaClassName')")
    }
}
