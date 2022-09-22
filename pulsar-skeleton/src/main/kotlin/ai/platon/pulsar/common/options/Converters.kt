package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import com.beust.jcommander.IStringConverter
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.lang3.tuple.Pair
import java.awt.Dimension
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class DurationConverter : IStringConverter<Duration> {
    override fun convert(value: String): Duration {
        return SParser(value).duration
    }
}

class InstantConverter : IStringConverter<Instant> {
    override fun convert(value: String): Instant {
        return SParser(value).getInstant(Instant.EPOCH)
    }
}

class PairConverter : IStringConverter<Pair<Int, Int>> {
    override fun convert(value: String): Pair<Int, Int> {
        val parts = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return Pair.of(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]))
    }
}

class BrowserTypeConverter : IStringConverter<BrowserType> {
    override fun convert(value: String): BrowserType {
        return BrowserType.fromString(value)
    }
}

class FetchModeConverter : IStringConverter<FetchMode> {
    override fun convert(value: String): FetchMode {
        return FetchMode.fromString(value)
    }
}

enum class ItemExtractor {
    DEFAULT, BOILERPIPE;

    override fun toString(): String {
        return name.lowercase(Locale.getDefault())
    }

    companion object {
        fun fromString(s: String?): ItemExtractor {
            if (s == null || s.isEmpty()) {
                return DEFAULT
            }

            try {
                return valueOf(s.uppercase(Locale.getDefault()))
            } catch (e: Throwable) {
                return DEFAULT
            }

        }
    }
}

class ItemExtractorConverter : IStringConverter<ItemExtractor> {
    override fun convert(value: String): ItemExtractor {
        return ItemExtractor.fromString(value)
    }
}

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WeightedKeywordsConverter : IStringConverter<Map<String, Double>> {
    override fun convert(value: String): Map<String, Double> {
        var value0 = value
        val keywords = HashMap<String, Double>()
        value0 = StringUtils.remove(value0, ' ')
        val parts = value0.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (part in parts) {
            var k = part
            var v = "1"

            val pos = part.indexOf('^')
            if (pos >= 1 && pos < part.length - 1) {
                k = part.substring(0, pos)
                v = part.substring(pos + 1)
            }

            keywords[k] = NumberUtils.toDouble(v, 1.0)
        }

        return keywords
    }
}

class IntRangeConverter : IStringConverter<IntRange> {
    override fun convert(value: String): IntRange {
        val (a, b) = value.lowercase(Locale.getDefault()).split("..".toRegex())
        return IntRange(a.toInt(), b.toInt())
    }
}

class DimensionConverter : IStringConverter<Dimension> {
    override fun convert(value: String): Dimension {
        val (a, b) = value.lowercase(Locale.getDefault()).split("x".toRegex())
        return Dimension(a.toInt(), b.toInt())
    }
}

enum class Condition {
    BEST, BETTER, GOOD, WORSE, WORST;

    companion object {
        fun valueOfOrDefault(s: String?): Condition {
            return try {
                valueOf(s?.uppercase(Locale.getDefault()) ?: "GOOD")
            } catch (e: Throwable) {
                GOOD
            }
        }
    }
}

class ConditionConverter : IStringConverter<Condition> {
    override fun convert(value: String) = Condition.valueOfOrDefault(value)
}
