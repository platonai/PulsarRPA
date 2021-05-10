package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.boilerpipe.utils.BoiConstants
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import java.sql.ResultSet
import java.sql.SQLException

@UDFGroup(namespace = "NEWS")
object NewsFunctions {

    @UDFunction
    @JvmStatic
    fun getPageTitle(rs: ResultSet): String {
        return get(rs, BoiConstants.DOC_FIELD_PAGE_TITLE)
    }

    @UDFunction @JvmStatic
    fun getContentTitle(rs: ResultSet): String {
        return get(rs, BoiConstants.DOC_FIELD_CONTENT_TITLE)
    }

    @UDFunction @JvmStatic
    fun getHtmlContent(rs: ResultSet): String {
        return get(rs, BoiConstants.DOC_FIELD_HTML_CONTENT)
    }

    @UDFunction @JvmStatic
    fun getTextContent(rs: ResultSet): String {
        return get(rs, BoiConstants.DOC_FIELD_TEXT_CONTENT)
    }

    @UDFunction @JvmStatic
    fun getPublishTime(rs: ResultSet): String {
        return get(rs, BoiConstants.DOC_FIELD_PUBLISH_TIME)
    }

    @UDFunction @JvmStatic
    fun getModifiedTime(rs: ResultSet): String {
        return get(rs, BoiConstants.DOC_FIELD_MODIFIED_TIME)
    }

    @UDFunction @JvmStatic
    operator fun get(rs: ResultSet, columnLabel: String): String {
        try {
            return rs.getString(columnLabel)
        } catch (e: SQLException) {
            throw RuntimeException(e.toString())
        }
    }
}
