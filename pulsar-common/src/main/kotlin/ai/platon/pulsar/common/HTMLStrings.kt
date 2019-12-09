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
