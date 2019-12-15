package ai.platon.pulsar.persist.metadata;

import org.jetbrains.annotations.NotNull;

/**
 * TODO: Auto detect fetch mode: first try native, and then try selenium, if there is no differences, use native mode
 * TODO: FetchMode seems can be merged into BrowserType
 */
public enum FetchMode {
    UNKNOWN, AUTO, NATIVE, PROXY, CROWDSOURCING, SELENIUM, NATIVE_RENDERER;

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
