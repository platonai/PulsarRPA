package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkType
import java.util.*

object ScrapeAPIUtils {

    private val allowedArgs = LoadOptions.apiPublicOptionNames
    private val allowedScrapeUDFs = arrayOf("loadandselect")

    @Throws(IllegalArgumentException::class)
    fun normalize(rawSql: String?): NormXSQL {
        if (rawSql == null) {
            throw throw IllegalArgumentException("SQL is required")
        }

        val configuredUrl = extractConfiguredUrl(rawSql).takeIf { URLUtils.isStandard(it) }
            ?: throw IllegalArgumentException("No url found in sql: >>>$rawSql<<<")

        val (url, args) = URLUtils.splitUrlArgs(configuredUrl)
        val sql = eraseExpireOptions(rawSql)

        return NormXSQL(url, args, sql)
    }

    @Throws(IllegalArgumentException::class)
    fun checkArgs(args: String?) {
        args?.split("\\s+".toRegex())?.filter { it.startsWith("-") }?.forEach { arg ->
            if (arg !in allowedArgs) {
                throw IllegalArgumentException("Argument is not allowed: <$arg>")
            }
        }
    }

    fun isScrapeUDF(sql: String?): Boolean {
        if (sql.isNullOrBlank()) {
            return false
        }

        val s = sql.replace("_", "").replace("\\s+", " ").lowercase()
        return allowedScrapeUDFs.any { it in s }
    }

    @Throws(IllegalArgumentException::class)
    fun checkSql(sql: String): String {
        return try {
            APISQLUtils.sanitize(sql)
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message)
        }
    }

    fun eraseUrlOptions(sql: String, vararg fields: String): String {
        // do not forget the blank
        val separator = " | "
        val optionNames = fields.flatMap { LoadOptions.getOptionNames(it) }.joinToString(separator)
        return sql.replace(optionNames.toRegex(), " -erased ")
    }

    fun eraseExpireOptions(sql: String): String {
        return eraseUrlOptions(sql, "expires", "expireAt", "itemExpires", "itemExpireAt")
    }

    /**
     * Extract the url from the SQL, the url might be configured
     * */
    fun extractConfiguredUrl(sql: String?): String? {
        if (sql == null) {
            return null
        }

        LinkExtractors.fromText(sql).firstOrNull() ?: return null

        val sql0 = checkSql(sql).replace("\\s+".toRegex(), " ")
        return if (sql0.contains(" from ", ignoreCase = true)) {
            ResultSetUtils.extractUrlFromFromClause(sql0).takeIf { URLUtils.isStandard(it) }
        } else {
            null
        }
    }
}
