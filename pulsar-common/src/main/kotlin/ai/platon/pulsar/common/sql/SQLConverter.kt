package ai.platon.pulsar.common.sql

import org.apache.commons.lang3.StringUtils

object SQLConverter {
    fun createSql2extractSql(createSql: String): String {
        val prefix = "select\n"
        val postfix = "\nfrom load_and_select(@url, '')"
        return createSql.split("\n")
                .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
                .map { StringUtils.substringBetween(it.trim(), "`", "`") }
                .map { "    dom_first_text(dom, 'div') as `$it`" }
                .joinToString(",\n", prefix, postfix) { it }
    }

    fun extractSql2createSql(extractSql: String, tableName: String): String {
        val prefix = "drop table `$tableName` if exists;\ncreate table `$tableName`(\n" +
                "    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',"
        val postfix = "\n) ENGINE=MYISAM DEFAULT CHARSET=utf8mb4 COMMENT='auto created table from x-sql';"
        return extractSql.split("\n")
                .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
                .map { StringUtils.substringAfterLast(it, " as ").removeSuffix(",").trim('`') }
                .filter { it.isNotBlank() }
                .map { "    `$it` varchar(255) default null" }
                .joinToString(",\n", prefix, postfix) { it }
    }
}
