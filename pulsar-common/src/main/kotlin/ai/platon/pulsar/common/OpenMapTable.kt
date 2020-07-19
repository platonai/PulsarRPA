package ai.platon.pulsar.common

import kotlin.reflect.KProperty

/**
 * A simple excel like table, every column, every row and every cell can hold metadata and variables
 * */
class OpenMapTable(
        val numColumns: Int,
        val ident: Int = -1
) {
    val metadata = Metadata(numColumns, ident)
    val map = mutableMapOf<String, Row>()

    val attributes = mutableMapOf<String, Any>()
    val columns get() = metadata.columns
    val rows get() = map.values

    val keys get() = map.keys
    val numRows: Int get() = map.size
    val isEmpty: Boolean get() = numRows == 0
    val isNotEmpty: Boolean get() = !isEmpty

    operator fun get(key: String) = map[key]

    operator fun set(key: String, row: Row) {
        map[key] = row
    }

    fun computeIfAbsent(key: String) = map.computeIfAbsent(key) { Row(key, numColumns) }

    fun computeIfAbsent(key: String, init: (Row) -> Unit) = map.computeIfAbsent(key) { Row(key, numColumns, init) }

    fun count(predicate: (Row) -> Boolean) = rows.count(predicate)

    class Metadata(val columns: Array<Column>) {

        constructor(numColumns: Int, tableId: Int = -1): this(
            IntRange(1, numColumns).map { i -> Column(if (tableId < 0) "C$i" else "T${tableId}C$i") }.toTypedArray()
        )

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

    class Cell(var j: Int = 0, var value: Any? = null) {
        val attributes: MutableMap<String, Any> = mutableMapOf()
        override fun toString() = value?.toString()?:""
    }

    class Row(var key: String = "", numColumns: Int) {
        constructor(key: String, numColumns: Int, init: (Row) -> Unit): this(key, numColumns) {
            init(this)
        }

        val cells: Array<Cell?> = arrayOfNulls(numColumns)
        val attributes: MutableMap<String, Any> = mutableMapOf()
        val values get() = cells.map { it?.value }

        operator fun get(j: Int) = cells[j]

        operator fun set(j: Int, cell: Cell) {
            require(j < cells.size)
            require(j == cell.j)
            cells[j] = cell
        }

        fun getValue(j: Int) = cells[j]?.value

        fun setValue(j: Int, value: Any?) {
            require(j < cells.size)
            cells[j] = Cell(j, value)
        }
    }

    companion object {
        val empty = OpenMapTable(0)
    }
}

class TableAttribute<T: Any>(val initializer: (OpenMapTable) -> T) {
    operator fun getValue(thisRef: OpenMapTable, property: KProperty<*>): T =
            thisRef.attributes[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: OpenMapTable, property: KProperty<*>, value: T): T {
        thisRef.attributes[property.name] = value
        return value
    }
}

class ColumnAttribute<T: Any>(val initializer: (OpenMapTable.Column) -> T) {
    operator fun getValue(thisRef: OpenMapTable.Column, property: KProperty<*>): T =
            thisRef.attributes[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: OpenMapTable.Column, property: KProperty<*>, value: T): T {
        thisRef.attributes[property.name] = value
        return value
    }
}

class RowAttribute<T: Any>(val initializer: (OpenMapTable.Row) -> T) {
    operator fun getValue(thisRef: OpenMapTable.Row, property: KProperty<*>): T =
            thisRef.attributes[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: OpenMapTable.Row, property: KProperty<*>, value: T): T {
        thisRef.attributes[property.name] = value
        return value
    }
}

class CellAttribute<T: Any>(val initializer: (OpenMapTable.Cell) -> T) {
    operator fun getValue(thisRef: OpenMapTable.Cell, property: KProperty<*>): T =
            thisRef.attributes[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: OpenMapTable.Cell, property: KProperty<*>, value: T): T {
        thisRef.attributes[property.name] = value
        return value
    }
}
