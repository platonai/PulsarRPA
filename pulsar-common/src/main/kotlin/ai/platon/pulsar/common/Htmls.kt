package ai.platon.pulsar.common

import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern

const val DEFAULT_SUPPORTED_CHARSETS = "UTF-8|GB2312|GB18030|GBK|Big5|ISO-8859-1" +
        "|windows-1250|windows-1251|windows-1252|windows-1253|windows-1254|windows-1257"
val DEFAULT_CHARSET_PATTERN = DEFAULT_SUPPORTED_CHARSETS.replace("UTF-8\\|?", "")
        .toPattern(Pattern.CASE_INSENSITIVE)
// All charsets are supported by the system
val SYSTEM_AVAILABLE_CHARSETS = Charset.availableCharsets().values.joinToString("|") { it.name() }
val SYSTEM_AVAILABLE_CHARSET_PATTERN = SYSTEM_AVAILABLE_CHARSETS.replace("UTF-8\\|?", "")
        .toPattern(Pattern.CASE_INSENSITIVE)

enum class HtmlIntegrity {
    OK,
    EMPTY_0B,  // no character at all
    EMPTY_39B, // <html><head></head><body></body></html> and blanks, must be caused by a bad proxy
    BLANK_BODY, // ...<body>\s*</body>...
    NO_ANCHOR,
    NO_JS_OK_FLAG,
    /**
     * the page displays captcha or something similar
     * */
    ROBOT_CHECK,
    /**
     * the page displays "404 Not Found" or something similar,
     * the the server should issue a 404 error code, but not guaranteed
     * */
    NOT_FOUND,
    WRONG_COUNTRY,
    WRONG_DISTRICT,
    WRONG_LANG,
    TOO_SMALL,
    TOO_SMALL_IN_HISTORY,
    TOO_SMALL_IN_BATCH,
    OTHER;

    val isOK: Boolean get() = this == OK
    val isNotOK: Boolean get() = !isOK

    val isEmpty: Boolean get() = this == EMPTY_0B || this == EMPTY_39B
    val isNotEmpty: Boolean get() = !isEmpty
    val isEmptyBody: Boolean get() = this == BLANK_BODY
    val isBanned: Boolean get() = this == ROBOT_CHECK
    val isNotFound: Boolean get() = this == NOT_FOUND
    val isSmall: Boolean get() = this == TOO_SMALL || this == TOO_SMALL_IN_HISTORY || this == TOO_SMALL_IN_BATCH
    val isOther get() = this == OTHER

    companion object {
        fun fromString(s: String?): HtmlIntegrity {
            return if (s == null || s.isEmpty()) {
                OK
            } else try {
                valueOf(s.uppercase(Locale.getDefault()))
            } catch (e: Throwable) {
                OK
            }
        }
    }
}

/**
 * Replace the charset to the target charset
 * */
fun replaceHTMLCharset(htmlContent: String, charsetPattern: Pattern, targetCharset: String = "UTF-8"): StringBuilder {
    val pos = htmlContent.indexOf("</head>")
    if (pos < 0) {
        return StringBuilder()
    }

    var head = htmlContent.take(pos)
    // Some parsers use html directive to decide the content's encoding, correct it to be UTF-8
    head = charsetPattern.matcher(head).replaceAll(targetCharset)

    // append the new head
    val sb = StringBuilder(head)
    // append all the rest
    sb.append(htmlContent, pos, htmlContent.length)

    return sb
}

fun hasHtmlTags(htmlContent: String): Boolean {
    return htmlContent.indexOf("<html") != -1 && htmlContent.lastIndexOf("</html>") != -1
}

fun hasHeadTags(htmlContent: String): Boolean {
    return htmlContent.indexOf("<head") != -1 && htmlContent.lastIndexOf("</head>") != -1
}

fun hasBodyTags(htmlContent: String): Boolean {
    return htmlContent.indexOf("<body") != -1 && htmlContent.lastIndexOf("</body>") != -1
}

fun isBlankBody(htmlContent: String): Boolean {
    val tagStart = "<body"
    val tagEnd = "</body>"

    val h = htmlContent
    var p = h.indexOf(tagStart) // pos of <body ...>
    p = h.indexOf(">", p) + 1

    while (h[p].isWhitespace() && p < h.length) {
        ++p
    }

    if (p + tagEnd.length > h.length) {
        return false
    }

    tagEnd.forEachIndexed { i, c ->
        if (c != h[p + i]) {
            return false
        }
    }

    return true
}
