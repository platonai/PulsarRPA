package ai.platon.pulsar.common.browser;

/**
 * The browser type
 */
public enum BrowserType {
    NATIVE, CHROME, MOCK_CHROME, SELENIUM_CHROME, PLAYWRIGHT_CHROME, PHANTOMJS;

    /**
     * Create a browser type from a string
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link BrowserType} object.
     */
    public static BrowserType fromString(String s) {
        if (s == null || s.isEmpty()) {
            return CHROME;
        }

        try {
            return valueOf(s.toUpperCase());
        } catch (Throwable e) {
            return CHROME;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
