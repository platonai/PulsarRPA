package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.metadata.Mark;
import org.apache.avro.util.Utf8;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JPersistUtils {

    /**
     * Return a Utf8 string.
     *
     * Unlike {@link String}, instances are mutable. This is more
     * efficient than {@link String} when reading or writing a sequence of values,
     * as a single instance may be reused.
     * */
    @NotNull
    public static Utf8 wrapKey(@NotNull String key) {
        return u8(key);
    }

    /**
     * Return a Utf8 string.
     * <p>
     * Unlike {@link String}, instances are mutable. This is more
     * efficient than {@link String} when reading or writing a sequence of values,
     * as a single instance may be reused.
     * */
    @NotNull
    public static Utf8 wrapKey(@NotNull Mark mark) {
        return u8(mark.value());
    }

    /**
     * Return a Utf8 string.
     * <p>
     * Unlike {@link String}, instances are mutable. This is more
     * efficient than {@link String} when reading or writing a sequence of values,
     * as a single instance may be reused.
     * */
    @Nullable
    public static Utf8 u8(@Nullable String value) {
        if (value == null) {
            // TODO: return new Utf8.EMPTY?
            return null;
        }
        return new Utf8(value);
    }
}
