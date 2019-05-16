package ai.platon.pulsar.common

import kotlin.reflect.KProperty

/**
 * A simple excel like table, every column, every row and every cell can hold metadata and variables
 * */
class OpenMapAnyTable(val numColumns: Int) {
    val metadata = Metadata(numColumns)
    val map = mutableMapOf<String, Row>()

    val attributes = mutableMapOf<String, Any>()
    val columns = metadata.columns
    val rows = map.values

    val keys = map.keys
    val numRows: Int get() = map.size
    val isEmpty: Boolean get() = numRows == 0
    val isNotEmpty: Boolean get() = !isEmpty

    operator fun get(key: String): Row? {
        return map[key]
    }

    operator fun set(key: String, row: Row) {
        map[key] = row
    }

    fun computeIfAbsent(key: String): Row {
        return map.computeIfAbsent(key) { Row(key, numColumns) }
    }

    fun computeIfAbsent(key: String, init: (Row) -> Unit): Row {
        return map.computeIfAbsent(key) { Row(key, numColumns, init) }
    }

    fun count(predicate: (Row) -> Boolean): Int {
        return rows.count(predicate)
    }

    class Metadata(numColumns: Int) {
        val columns = IntRange(1, numColumns)
                .map { Column("C$it") }.toTypedArray()
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

    class Column(
            var name: String = "",
            var description: String = "",
            val attributes: MutableMap<String, Any> = mutableMapOf()
    )

    class Cell(val value: Any? = null) {
        val attributes: MutableMap<String, Any> = mutableMapOf()
        override fun toString(): String {
            return value?.toString()?:""
        }
    }

    class Row(var key: String = "", numColumns: Int) {
        constructor(key: String, numColumns: Int, init: (Row) -> Unit): this(key, numColumns) {
            init(this)
        }

        val cells: Array<Cell?> = arrayOfNulls(numColumns)
        val attributes: MutableMap<String, Any> = mutableMapOf()
        val values get() = cells.map { it?.value }
        operator fun get(j: Int): Any? {
            return cells[j]?.value
        }
        operator fun set(j: Int, value: Any?) {
            require(j < cells.size)
            cells[j] = Cell(value)
        }
    }
}

class TableAttribute<T: Any>(val initializer: (OpenMapAnyTable) -> T) {
    operator fun getValue(thisRef: OpenMapAnyTable, property: KProperty<*>): T =
            thisRef.attributes[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: OpenMapAnyTable, property: KProperty<*>, value: T): T {
        thisRef.attributes[property.name] = value
        return value
    }
}

class ColumnAttribute<T: Any>(val initializer: (OpenMapAnyTable.Column) -> T) {
    operator fun getValue(thisRef: OpenMapAnyTable.Column, property: KProperty<*>): T =
            thisRef.attributes[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: OpenMapAnyTable.Column, property: KProperty<*>, value: T): T {
        thisRef.attributes[property.name] = value
        return value
    }
}

class RowAttribute<T: Any>(val initializer: (OpenMapAnyTable.Row) -> T) {
    operator fun getValue(thisRef: OpenMapAnyTable.Row, property: KProperty<*>): T =
            thisRef.attributes[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: OpenMapAnyTable.Row, property: KProperty<*>, value: T): T {
        thisRef.attributes[property.name] = value
        return value
    }
}

class CellAttribute<T: Any>(val initializer: (OpenMapAnyTable.Cell) -> T) {
    operator fun getValue(thisRef: OpenMapAnyTable.Cell, property: KProperty<*>): T =
            thisRef.attributes[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: OpenMapAnyTable.Cell, property: KProperty<*>, value: T): T {
        thisRef.attributes[property.name] = value
        return value
    }
}
