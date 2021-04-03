package ai.platon.pulsar.common.sql

import ai.platon.pulsar.common.ResourceLoader
import org.apache.commons.lang3.StringUtils

object SqlResourceUtils {
    /**
     * Load sql and convert column name
     * A convert column name is like AS `Breadcrumbs last link -> category`
     * */
    fun loadConvertSql(fileResource: String): String {
        return ResourceLoader.readAllLines(fileResource)
                .asSequence()
                .filterNot { it.trim().startsWith("-- ") }
                .map { it.substringBeforeLast("-- ") }
                .filter { !it.contains("as") || it.contains(" -> ") }
                .map { sql ->
                    StringUtils.substringBetween(sql, "`", " -> ")?.let { sql.replace("$it -> ", "") } ?: sql
                }
                .filterNot { it.isBlank() }
                .joinToString("\n") { it }
    }

    fun loadSql(fileResource: String): String {
        return ResourceLoader.readAllLines(fileResource)
                .asSequence()
                .filterNot { it.trim().startsWith("-- ") }
                .map { it.substringBeforeLast("-- ") }
                .filterNot { it.isBlank() }
                .joinToString("\n") { it }
    }
}
