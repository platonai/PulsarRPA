package ai.platon.pulsar.persist.metadata;

/**
 * <p>BrowserType class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public enum BrowserType {
    NATIVE, CHROME, MOCK_CHROME, SELENIUM_CHROME, PLAYWRIGHT_CHROME, PHANTOMJS;

    /**
     * <p>fromString.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.persist.metadata.BrowserType} object.
     */
    public static BrowserType fromString(String s) {
        if (s == null || s.isEmpty()) {
            return NATIVE;
        }

        try {
            return valueOf(s.toUpperCase());
        } catch (Throwable e) {
            return NATIVE;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
