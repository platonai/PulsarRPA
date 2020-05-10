package ai.platon.pulsar.common

import org.apache.commons.lang3.Validate
import java.net.MalformedURLException

class TenantedUrl(val tenantId: Int, var url: String) : Comparable<TenantedUrl> {

    fun reverseUrl(): TenantedUrl {
        this.url = Urls.reverseUrl(url)
        return this
    }

    fun unreverseUrl(): TenantedUrl {
        this.url = Urls.unreverseUrl(url)
        return this
    }

    fun checkTenant(tenantId: Int): Boolean {
        return this.tenantId == tenantId
    }

    override fun toString(): String {
        return combine(tenantId, url)
    }

    override fun hashCode(): Int {
        return tenantId + url.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false

        val otherTUrl = other as TenantedUrl?
        return tenantId == otherTUrl!!.tenantId && url == otherTUrl.url
    }

    override fun compareTo(other: TenantedUrl): Int {
        return toString().compareTo(other.toString())
    }

    companion object {

        val TENANT_ID_SEPERATOR = '-'

        /**
         * Quick check if a url is tenanted
         *
         * @param url the url
         * @return if the url start with a digit number, returns true
         */
        fun isTenanted(url: String): Boolean {
            return url.isNotEmpty() && Character.isDigit(url[0])
        }

        fun of(tenantId: Int, url: String): TenantedUrl {
            return TenantedUrl(tenantId, url)
        }

        /**
         * construct a new TenantedUrl from a url : tenant id and normal url, if the url is not tenanted,
         * returned tenant id is 0
         *
         * @throws MalformedURLException
         */
        fun split(url: String): TenantedUrl {
            if (url.isEmpty() || !Character.isDigit(url[0])) {
                return TenantedUrl(0, url)
            }

            val integerBuffer = StringBuilder()

            var pos = 0
            var ch: Char? = url[pos]
            while (pos < url.length && Character.isDigit(ch!!)) {
                integerBuffer.append(ch)
                ch = url[++pos]
            }

            if (url[pos] != TENANT_ID_SEPERATOR) {
                throw MalformedURLException("Url starts with numbers")
            }

            // skip TENANT_ID_SEPERATOR
            ++pos

            return TenantedUrl(Integer.parseInt(integerBuffer.toString()), url.substring(pos))
        }

        /**
         * Combine tenantId and untenantedUrl to a tenanted url representation
         *
         *
         * Zero tenant id means no tenant, so return the original untenantedUrl
         *
         * @param untenantedUrl the untenanted url, the caller should ensure it's not tenanted
         * @return the tenanted url of untenantedUrl
         */
        fun combine(tenantId: Int, untenantedUrl: String): String {
            Validate.isTrue(!isTenanted(untenantedUrl))

            if (tenantId == 0) {
                return untenantedUrl
            }

            val buf = StringBuilder()
            buf.append(tenantId)
            buf.append(TENANT_ID_SEPERATOR)
            buf.append(untenantedUrl)
            return buf.toString()
        }

        /**
         * Get url part of tenantedUrl, if it has a tenant id, strip the tenant id, otherwise,
         *
         * @return the url part of a tenantedUrl
         * @throws MalformedURLException
         */
        fun stripTenant(url: String): String {
            return split(url).url
        }
    }
}
