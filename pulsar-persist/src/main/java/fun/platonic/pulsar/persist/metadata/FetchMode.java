package fun.platonic.pulsar.persist.metadata;

/**
 * TODO: Auto detect fetch mode. First try native, and then try selenium, if there is no differences, use native mode
 */
public enum FetchMode {
    UNKNOWN, AUTO, NATIVE, PROXY, CROWDSOURCING, SELENIUM;

    public static FetchMode fromString(String s) {
        if (s == null || s.isEmpty()) {
            return UNKNOWN;
        }

        try {
            return FetchMode.valueOf(s.toUpperCase());
        } catch (Throwable e) {
            return UNKNOWN;
        }
    }
}
