package ai.platon.pulsar.persist.metadata;

import org.jetbrains.annotations.NotNull;

/**
 * TODO: Auto detect fetch mode: first try native, and then try selenium, if there is no differences, use native mode
 * TODO: FetchMode seems can be merged into BrowserType
 */
public enum FetchMode {
    UNKNOWN,
    /**
     * Simple native fetcher, no script renderer/cookie supported
     * */
    NATIVE,
    /**
     * Native renderer, for example, jbrowserdriver
     * */
    NATIVE_RENDERER,
    /**
     * Crowd source
     * */
    CROWD_SOURCING,
    /**
     * Selenium or selenium compatible protocol
     * */
    SELENIUM,
    /**
     * Choose the fetcher automatically
     * Not implemented
     * */
    AUTO;

    @NotNull
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
