package ai.platon.pulsar.common

import java.nio.charset.Charset
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
    OK, EMPTY, EMPTY_BODY, NO_BODY_START, NO_BODY_END, NO_ANCHOR, NO_JS_OK,
    TOO_SMALL, TOO_SMALL_IN_HISTORY, TOO_SMALL_IN_BATCH;

    val isOK: Boolean get() = this == OK
    val isNotOK: Boolean get() = !isOK

    companion object {
        fun fromString(s: String?): HtmlIntegrity {
            return if (s == null || s.isEmpty()) {
                OK
            } else try {
                valueOf(s.toUpperCase())
            } catch (e: Throwable) {
                OK
            }
        }
    }
}

/**
 * Replace the charset to the target charset
 * */
fun replaceHTMLCharset(pageSource: String, charsetPattern: Pattern, targetCharset: String = "UTF-8"): StringBuilder {
    val pos = pageSource.indexOf("</head>")
    if (pos < 0) {
        return StringBuilder()
    }

    var head = pageSource.take(pos)
    // Some parsers use html directive to decide the content's encoding, correct it to be UTF-8
    head = charsetPattern.matcher(head).replaceAll(targetCharset)

    // append the rest
    val sb = StringBuilder(head)
    sb.append(pageSource, pos, pageSource.length)

    return sb
}

fun hasHtmlTags(pageSource: String): Boolean {
    return pageSource.indexOf("<html") != -1 && pageSource.lastIndexOf("</html>") != -1
}

fun hasHeadTags(pageSource: String): Boolean {
    return pageSource.indexOf("<head") != -1 && pageSource.lastIndexOf("</head>") != -1
}

fun hasBodyTags(pageSource: String): Boolean {
    return pageSource.indexOf("<body") != -1 && pageSource.lastIndexOf("</body>") != -1
}

fun isEmptyBody(pageSource: String): Boolean {
    return pageSource.indexOf("<body></body>") != -1
}
