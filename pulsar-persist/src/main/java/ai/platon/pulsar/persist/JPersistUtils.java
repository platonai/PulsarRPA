package ai.platon.pulsar.persist;

import org.apache.avro.util.Utf8;
import org.jetbrains.annotations.Nullable;

public class JPersistUtils {

    /**
     * Return a Utf8 string.
     * <p>
     * Unlike {@link String}, instances are mutable. This is more
     * efficient than {@link String} when reading or writing a sequence of values,
     * as a single instance may be reused.
     */
    @Nullable
    public static Utf8 u8(@Nullable String value) {
        if (value == null) {
            // TODO: return new Utf8.EMPTY?
            return null;
        }
        return new Utf8(value);
    }
}
