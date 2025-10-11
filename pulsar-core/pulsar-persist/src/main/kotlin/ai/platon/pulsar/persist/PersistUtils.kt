package ai.platon.pulsar.persist

import org.apache.avro.util.Utf8

object PersistUtils {

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
            return null
        }
        return Utf8(value)
    }
}