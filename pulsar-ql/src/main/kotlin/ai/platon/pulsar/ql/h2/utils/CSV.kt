package ai.platon.pulsar.ql.h2.utils

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import org.apache.commons.lang3.RandomStringUtils
import java.nio.file.Files
import java.nio.file.Path
import java.sql.ResultSet
import java.sql.SQLException

class CSV(
    val separator: String = ",",
    val replacement: String = ";;"
) {
    @Throws(SQLException::class)
    fun toCSV(rs: ResultSet): String {
        val sb = StringBuilder()

        val headers = ResultSetUtils.getColumnNames(rs)
        headers.joinTo(sb, separator)
        sb.appendLine()

        val entities = ResultSetUtils.getTextEntitiesFromResultSet(rs)
        entities.forEach { entity ->
            entity.values.joinTo(sb, separator) { it.toString().replace(separator, replacement) }
            sb.appendLine()
        }

        rs.beforeFirst()

        return sb.toString()
    }

    @Throws(SQLException::class)
    fun toCSV(resultsets: Iterable<ResultSet>): String {
        val sb = StringBuilder()
        resultsets.forEachIndexed { i, rs ->
            if (i == 0) {
                val headers = ResultSetUtils.getColumnNames(rs).map { it.replace(separator, replacement) }
                headers.joinTo(sb, separator)
                sb.appendLine()
            }

            val entities = ResultSetUtils.getTextEntitiesFromResultSet(rs)
            entities.forEach { entity ->
                entity.values.joinTo(sb, separator) { it.toString().replace(separator, replacement) }
                sb.appendLine()
            }

            rs.beforeFirst()
        }
        return sb.toString()
    }

    fun export(rs: ResultSet,
                    prefix: String = "",
                    postfix: String = ".csv"
    ): Path {
        val now = DateTimes.formatNow("HH")
        val filename = RandomStringUtils.randomAlphabetic(5)
        val path = AppPaths.getTmp("rs").resolve(now).resolve("$prefix$filename$postfix")
        return export(rs, path)
    }

    fun export(rs: ResultSet, path: Path): Path {
        Files.createDirectories(path.parent)
        Files.writeString(path, toCSV(rs))
        return path
    }

    fun export(resultsets: Iterable<ResultSet>,
                    prefix: String = "",
                    postfix: String = ".csv"
    ): Path {
        val now = DateTimes.formatNow("HH")
        val filename = RandomStringUtils.randomAlphabetic(5)
        val path = AppPaths.getTmp("rs").resolve(now).resolve("$prefix$filename$postfix")
        return export(resultsets, path)
    }

    fun export(resultsets: Iterable<ResultSet>, path: Path): Path {
        Files.createDirectories(path.parent)
        val csv = toCSV(resultsets)

        Files.writeString(path, csv)
        return path
    }
}
