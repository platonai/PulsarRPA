package ai.platon.pulsar.common.http

import java.io.Serializable


/**
 * Default implementation of [HttpStatusCode].
 *
 * @author Arjen Poutsma
 * @since 6.0
 */
class DefaultHttpStatusCode(
        override val value: Int
) : HttpStatusCode, Comparable<HttpStatusCode>, Serializable {

    override fun is1xxInformational(): Boolean {
        return hundreds() == 1
    }

    override fun is2xxSuccessful(): Boolean {
        return hundreds() == 2
    }

    override fun is3xxRedirection(): Boolean {
        return hundreds() == 3
    }

    override fun is4xxClientError(): Boolean {
        return hundreds() == 4
    }

    override fun is5xxServerError(): Boolean {
        return hundreds() == 5
    }

    override fun isError(): Boolean {
        val hundreds = hundreds()
        return hundreds == 4 || hundreds == 5
    }

    private fun hundreds(): Int {
        return value / 100
    }

    override operator fun compareTo(o: HttpStatusCode): Int {
        return value.compareTo(o.value)
    }

    override fun hashCode(): Int {
        return value
    }

    override fun equals(obj: Any?): Boolean {
        return this === obj || obj is HttpStatusCode && value == obj.value
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        private const val serialVersionUID = 7017664779360718111L
    }
}
