package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.common.urls.UrlUtils;
import ai.platon.pulsar.persist.metadata.Mark;
import ai.platon.pulsar.persist.metadata.Name;
import ai.platon.pulsar.persist.model.WebPageFormatter;
import org.apache.avro.util.Utf8;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The core web page structure
 */
public abstract class AbstractWebPage implements MutableWebPage {
    private static final AtomicInteger SEQUENCER = new AtomicInteger();

    protected Integer id;
    /**
     * The url is the permanent internal address, and the location is the last working address
     */
    @NotNull
    protected String url;
    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this object
     */
    @NotNull
    protected String reversedUrl;
    /**
     * Web page scope configuration
     */
    @NotNull
    protected VolatileConfig conf;
    /**
     * Web page scope variables
     */
    protected final Variables variables = new Variables();

    /**
     * If this page is fetched from Internet
     */
    protected boolean isCached = false;

    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    protected boolean isLoaded = false;

    /**
     * If this page is fetched from Internet
     */
    protected boolean isFetched = false;

    /**
     * If this page is canceled
     */
    protected boolean isCanceled = false;

    /**
     * If this page is fetched and updated
     */
    protected volatile boolean isContentUpdated = false;

    /**
     * The cached content
     */
    protected volatile ByteBuffer tmpContent = null;

    /**
     * The delay time to retry if a retry is needed
     */
    protected Duration retryDelay = Duration.ZERO;

    protected AbstractWebPage(
            @NotNull String url, boolean urlReversed, @NotNull VolatileConfig conf
    ) {
        this.id = SEQUENCER.incrementAndGet();
        this.url = urlReversed ? UrlUtils.unreverseUrl(url) : url;
        this.reversedUrl = urlReversed ? url : UrlUtils.reverseUrlOrEmpty(url);
    }

    protected AbstractWebPage(
            @NotNull String url, @NotNull String reversedUrl, @NotNull VolatileConfig conf
    ) {
        this.id = SEQUENCER.incrementAndGet();
        this.url = url;
        this.reversedUrl = reversedUrl;
        this.conf = conf;
    }

    @NotNull
    public static Utf8 wrapKey(@NotNull Mark mark) {
        return u8(mark.value());
    }

    @Nullable
    public static Utf8 u8(@Nullable String value) {
        if (value == null) {
            // TODO: return new Utf8.EMPTY?
            return null;
        }
        return new Utf8(value);
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    @NotNull
    public String getKey() {
        return getReversedUrl();
    }

    @NotNull
    public String getReversedUrl() {
        return reversedUrl;
    }

    public int getId() {
        return id;
    }

    /**
     * Get The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     * <p>
     * TODO: use a separate field for href
     *
     * @return The hypertext reference
     */
    @Nullable
    public String getHref() {
        return getMetadata().get(Name.HREF);
    }

    /**
     * Set The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     *
     * @param href The hypertext reference
     */
    public void setHref(@Nullable String href) {
        getMetadata().set(Name.HREF, href);
    }

    public boolean isInternal() {
        return hasMark(Mark.INTERNAL.value());
    }

    public boolean isNotInternal() {
        return !isInternal();
    }

    /**
     * *****************************************************************************
     * Common fields
     * ******************************************************************************
     */

    /**
     * Get the cached content
     */
    @Nullable
    public ByteBuffer getTmpContent() {
        return tmpContent;
    }

    /**
     * Set the cached content, keep the persisted page content unmodified
     */
    public void setTmpContent(ByteBuffer tmpContent) {
        this.tmpContent = tmpContent;
    }

    @NotNull
    public Variables getVariables() {
        return variables;
    }

    /**
     * Check if the page scope temporary variable with {@code name} exists
     *
     * @param name The variable name to check
     * @return true if the variable exist
     */
    public boolean hasVar(@NotNull String name) {
        return variables.contains(name);
    }

    /**
     * Returns the local variable value to which the specified name is mapped,
     * or {@code null} if the local variable map contains no mapping for the name.
     *
     * @param name the name whose associated value is to be returned
     * @return the value to which the specified name is mapped, or
     *         {@code null} if the local variable map contains no mapping for the key
     */
    public Object getVar(@NotNull String name) {
        return variables.get(name);
    }

    /**
     * Get a page scope temporary variable
     *
     * @param name  The variable name.
     * @param value The variable value.
     */
    public void setVar(@NotNull String name, @NotNull Object value) {
        variables.set(name, value);
    }

    /**
     * Retrieves and removes the local variable with the given name.
     */
    public Object removeVar(@NotNull String name) {
        return variables.remove(name);
    }

    public boolean isCached() {
        return isCached;
    }

    public void setCached(boolean cached) {
        isCached = cached;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public boolean isFetched() {
        return isFetched;
    }

    public void setFetched(boolean fetched) {
        isFetched = fetched;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }

    public boolean isContentUpdated() {
        return isContentUpdated;
    }

    @NotNull
    public VolatileConfig getConf() {
        return conf;
    }

    public void setConf(@NotNull VolatileConfig conf) {
        this.conf = conf;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public int compareTo(@NotNull WebPage o) {
        return url.compareTo(o.getUrl());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof WebPage && ((WebPage) other).getUrl().equals(url);
    }

    @Override
    public String toString() {
        return new WebPageFormatter(this).format();
    }
}
