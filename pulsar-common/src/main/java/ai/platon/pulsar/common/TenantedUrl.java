package ai.platon.pulsar.common;

import org.apache.commons.lang3.Validate;

import java.net.MalformedURLException;

public class TenantedUrl implements Comparable<TenantedUrl> {

    public static final Character TENANT_ID_SEPERATOR = '-';

    private int tenantId;
    private String url;

    public TenantedUrl(int tenantId, String url) {
        this.tenantId = tenantId;
        this.url = url;
    }

    /**
     * Quick check if a url is tenanted
     *
     * @param url the url
     * @return if the url start with a digit number, returns true
     */
    public static boolean isTenanted(String url) {
        return !url.isEmpty() && Character.isDigit(url.charAt(0));
    }

    public static TenantedUrl of(int tenantId, String url) {
        return new TenantedUrl(tenantId, url);
    }

    /**
     * construct a new TenantedUrl from a url : tenant id and normal url, if the url is not tenanted,
     * returned tenant id is 0
     *
     * @throws MalformedURLException
     */
    public static TenantedUrl split(String url) throws MalformedURLException {
        if (url.isEmpty() || !Character.isDigit(url.charAt(0))) {
            return new TenantedUrl(0, url);
        }

        StringBuilder integerBuffer = new StringBuilder();

        int pos = 0;
        Character ch = url.charAt(pos);
        while (pos < url.length() && Character.isDigit(ch)) {
            integerBuffer.append(ch);
            ch = url.charAt(++pos);
        }

        if (url.charAt(pos) != TENANT_ID_SEPERATOR) {
            throw new MalformedURLException("Url starts with numbers");
        }

        // skip TENANT_ID_SEPERATOR
        ++pos;

        return new TenantedUrl(Integer.parseInt(integerBuffer.toString()), url.substring(pos));
    }

    /**
     * Combine tenantId and untenantedUrl to a tenanted url representation
     * <p>
     * Zero tenant id means no tenant, so return the original untenantedUrl
     *
     * @param untenantedUrl the untenanted url, the caller should ensure it's not tenanted
     * @return the tenanted url of untenantedUrl
     */
    public static String combine(int tenantId, String untenantedUrl) {
        Validate.isTrue(!isTenanted(untenantedUrl));

        if (tenantId == 0) {
            return untenantedUrl;
        }

        StringBuilder buf = new StringBuilder();
        buf.append(tenantId);
        buf.append(TENANT_ID_SEPERATOR);
        buf.append(untenantedUrl);
        return buf.toString();
    }

    /**
     * Get url part of tenantedUrl, if it has a tenant id, strip the tenant id, otherwise,
     *
     * @return the url part of a tenantedUrl
     * @throws MalformedURLException
     */
    public static String stripTenant(String url) throws MalformedURLException {
        return split(url).getUrl();
    }

    public int getTenantId() {
        return tenantId;
    }

    public String getUrl() {
        return url;
    }

    public TenantedUrl reverseUrl() throws MalformedURLException {
        this.url = UrlUtil.reverseUrl(url);
        return this;
    }

    public TenantedUrl unreverseUrl() throws MalformedURLException {
        this.url = UrlUtil.unreverseUrl(url);
        return this;
    }

    public boolean checkTenant(int tenantId) {
        return this.tenantId == tenantId;
    }

    @Override
    public String toString() {
        return combine(tenantId, url);
    }

    @Override
    public int hashCode() {
        return tenantId + url.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (getClass() != other.getClass()) return false;

        TenantedUrl otherTUrl = (TenantedUrl) other;
        return tenantId == otherTUrl.tenantId && url.equals(otherTUrl.url);
    }

    @Override
    public int compareTo(TenantedUrl other) {
        return toString().compareTo(other.toString());
    }
}
