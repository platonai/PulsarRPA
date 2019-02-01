package ai.platon.pulsar.persist.metadata;

public enum BrowserType {
    NATIVE, CHROME, HTMLUNIT, PHANTOMJS;

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
}
