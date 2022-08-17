package ai.platon.pulsar.persist.metadata;

import org.jetbrains.annotations.NotNull;

/**
 * @author vincent
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
