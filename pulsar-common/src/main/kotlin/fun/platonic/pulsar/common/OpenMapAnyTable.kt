package `fun`.platonic.pulsar.common

import kotlin.reflect.KClass

class OpenMapAnyTable(val numColumns: Int, var score: Double = 0.0, defaultCellType: KClass<out Any> = Any::class) {
    val metadata = Metadata(numColumns, defaultCellType)
    val map = mutableMapOf<String, Row>()

    val columns = metadata.columns
    val rows = map.values

    val keys = map.keys
    val numRows: Int get() = map.size

    operator fun get(key: String): Row? {
        return map[key]
    }

    operator fun set(key: String, row: Row) {
        map[key] = row
    }

    fun computeIfAbsent(key: String): Row {
        return map.computeIfAbsent(key) { Row(key, numColumns) }
    }

    fun computeIfAbsent(key: String, mapping: (String) -> Row): Row {
        return map.computeIfAbsent(key) { mapping(key) }
    }

    fun count(predicate: (Row) -> Boolean): Int {
        return rows.count(predicate)
    }

    class Column(
            var name: String = "",
            var description: String = "",
            var cellType: KClass<out Any> = Any::class,
            val attributes: MutableMap<String, Any> = mutableMapOf()
    )

    class Metadata(numColumns: Int, val defaultCellType: KClass<out Any> = Any::class) {
        val columns = IntRange(1, numColumns)
                .map { Column("C$it", cellType = defaultCellType) }.toTypedArray()
        operator fun get(j: Int): Column {
            return columns[j]
        }
        operator fun set(j: Int, column: Column) {
            columns[j] = column
        }
        fun attributeRow(key: String): List<Any?> {
            return columns.map { it.attributes[key] }
        }
    }

    class Row(var key: String = "", numColumns: Int) {
        val data: Array<Any?> = arrayOfNulls(numColumns)
        operator fun get(j: Int): Any? {
            return data[j]
        }
        operator fun set(i: Int, value: Any?) {
            data[i] = value
        }
    }
}
