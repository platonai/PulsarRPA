package ai.platon.pulsar.persist.metadata;

public enum BrowserType {
    NATIVE, CHROME, SELENIUM_CHROME, PHANTOMJS;

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

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
