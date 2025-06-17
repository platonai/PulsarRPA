package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.StringUtils

object SQLConverter {
    fun createSQL2extractSQL(createSQL: String): String {
        val prefix = "select\n"
        val postfix = "\nfrom load_and_select(@url, '')"
        return createSQL.split("\n")
                .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
                .map { StringUtils.substringBetween(it.trim(), "`", "`") }
                .map { "    dom_first_text(dom, 'div') as `$it`" }
                .joinToString(",\n", prefix, postfix) { it }
    }

    fun extractSQL2createSQL(extractSQL: String, tableName: String): String {
        val prefix = "drop table if exists `$tableName`;\ncreate table `$tableName`(\n" +
                "    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',"
        val postfix = "\n) DEFAULT CHARSET=utf8mb4 COMMENT='auto created table from x-sql';"

        return extractSQL.split("\n")
            .asSequence()
            .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
            .map { StringUtils.substringAfterLast(it, " as ").removeSuffix(",").trim('`') }
            .filter { it.isNotBlank() }
            .map { "    `$it` varchar(255) default null" }
            .joinToString(",\n", prefix, postfix) { it }
    }
}
