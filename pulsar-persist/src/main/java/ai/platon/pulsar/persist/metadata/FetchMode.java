package ai.platon.pulsar.persist.metadata;

import org.jetbrains.annotations.NotNull;

/**
 * TODO: Auto detect fetch mode: first try native, and then try selenium, if there is no differences, use native mode
 * TODO: FetchMode seems can be merged into BrowserType
 *
 * @author vincent
 * @version $Id: $Id
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
     * Fetch every page using a real browser
     * */
    BROWSER,
    /**
     * Choose the fetcher automatically
     * Not implemented
     * */
    AUTO;

    /**
     * <p>fromString.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.persist.metadata.FetchMode} object.
     */
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
