package ai.platon.pulsar.persist

import ai.platon.pulsar.persist.metadata.Mark
import org.apache.avro.util.Utf8

object PersistUtils {

    /**
     * Return a Utf8 string.
     *
     * Unlike [String], instances are mutable. This is more
     * efficient than [String] when reading or writing a sequence of values,
     * as a single instance may be reused.
     */
    fun wrapKey(key: String): Utf8 {
        return u8(key)!!
    }

    /**
     * Return a Utf8 string.
     *
     *
     * Unlike [String], instances are mutable. This is more
     * efficient than [String] when reading or writing a sequence of values,
     * as a single instance may be reused.
     */
    fun wrapKey(mark: Mark): Utf8 {
        return u8(mark.value())!!
    }

    /**
     * Return a Utf8 string.
     *
     *
     * Unlike [String], instances are mutable. This is more
     * efficient than [String] when reading or writing a sequence of values,
     * as a single instance may be reused.
     */
    fun u8(value: String?): Utf8? {
        if (value == null) {
            // TODO: return new Utf8.EMPTY?
            return null
        }
        return Utf8(value)
    }
}