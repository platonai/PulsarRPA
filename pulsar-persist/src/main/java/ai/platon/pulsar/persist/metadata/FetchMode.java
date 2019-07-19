package ai.platon.pulsar.persist.metadata;

import javax.annotation.Nonnull;

/**
 * TODO: Auto detect fetch mode. First try native, and then try selenium, if there is no differences, use native mode
 * TODO: FetchMode seems can be merged into BrowserType
 */
public enum FetchMode {
    UNKNOWN, AUTO, NATIVE, PROXY, CROWDSOURCING, SELENIUM;

    @Nonnull
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
