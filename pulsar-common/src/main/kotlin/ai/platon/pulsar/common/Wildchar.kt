package ai.platon.pulsar.common

class Wildchar(val wildcard: String) {
    fun toRegex(): Regex {
        return toRegularExpression().toRegex()
    }

    fun toRegularExpression(): String {
        val length = wildcard.length
        val s = StringBuffer(length)
        s.append('^')
        var i = 0
        while (i < length) {
            val c = wildcard[i]
            when (c) {
                '*' -> s.append(".*")
                '?' -> s.append(".")
                '(', ')', '[', ']', '$', '^', '.', '{', '}', '|', '\\' -> {
                    s.append("\\")
                    s.append(c)
                }
                else -> s.append(c)
            }
            i++
        }
        s.append('$')
        return s.toString()
    }
}
