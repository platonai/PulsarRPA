package ai.platon.pulsar.common.sql

import ai.platon.pulsar.common.ResourceLoader
import org.apache.commons.lang3.StringUtils

object SQLUtils {
    /**
     * A single quote is replaced to be %27 by URLEncoder, to properly handle a url-encoded url, we should choose
     * another placeholder
     * */
    const val SINGLE_QUOTE_PLACE_HOLDER = "^27"

    /**
     * Sanitize an url before it can be used in an X-SQL, e.g.
     * https://www.amazon.com/s?k=Baby+Girls'+One-Piece+Footies&rh=node:2475809011&page=1
     * is sanitized to be
     * https://www.amazon.com/s?k=Baby+Girls^27+One-Piece+Footies&rh=node:2475809011&page=1
     * */
    fun sanitizeUrl(url: String): String {
        return url.replace("'", SINGLE_QUOTE_PLACE_HOLDER)
    }

    fun unsanitizeUrl(sanitizedUrl: String): String {
        return sanitizedUrl.replace(SINGLE_QUOTE_PLACE_HOLDER, "'")
    }

    /**
     * Load sql and convert column name
     * A convert column name is like AS `Breadcrumbs last link -> category`
     * */
    fun loadConvertSQL(fileResource: String): String {
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

    fun loadSQL(fileResource: String): String {
        return ResourceLoader.readAllLines(fileResource)
                .asSequence()
                .filterNot { it.trim().startsWith("-- ") }
                .map { it.substringBeforeLast("-- ") }
                .filterNot { it.isBlank() }
                .joinToString("\n") { it }
    }
}
