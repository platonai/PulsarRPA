package ai.platon.pulsar.common.options.deprecated

import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.PulsarOptions
import com.beust.jcommander.DynamicParameter
import java.util.HashMap
import ai.platon.pulsar.common.options.deprecated.EntityOptions
import com.beust.jcommander.Parameter
import org.apache.commons.lang3.tuple.Pair
import java.lang.ClassCastException
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
@Deprecated("Use Web SQL instead")
class EntityOptions : PulsarOptions {
    @Parameter(names = ["-en", "--entity-name"], description = "The entity name.")
    var name: String = ""

    @Parameter(names = ["-er", "--entity-root"], description = "The entity's container path.")
    var root: String = ""

    @DynamicParameter(names = ["-F"], description = "Pulsar field extractors to extract sub entity fields")
    var cssRules: MutableMap<String, String> = HashMap()

    @DynamicParameter(names = ["-X"], description = "XPath selectors")
    var xpathRules: MutableMap<String, String> = HashMap()

    @DynamicParameter(names = ["-R"], description = "Regex selectors")
    var regexRules: MutableMap<String, String> = HashMap()
    var collectionOptions = CollectionOptions()

    protected constructor() {
        addObjects(this, collectionOptions)
    }

    protected constructor(args: String) : super(args!!) {
        addObjects(this, collectionOptions)
    }

    fun hasRules(): Boolean {
        return !cssRules.isEmpty() || !xpathRules.isEmpty() || !regexRules.isEmpty() || collectionOptions.hasRules()
    }

    override fun getParams(): Params {
        val fieldsParams = cssRules.entries.stream()
            .map { e: Map.Entry<String, String> -> "-F" + e.key + "=" + e.value }
            .collect(Collectors.toMap(
                Function { obj: String -> obj }, Function<String, Any> { v: String -> "" }))
        fieldsParams.putAll(xpathRules.entries.stream()
            .map { e: Map.Entry<String, String> -> "-X" + e.key + "=" + e.value }
            .collect(Collectors.toMap(
                Function { obj: String -> obj }, Function { v: String -> "" })))
        fieldsParams.putAll(regexRules.entries.stream()
            .map { e: Map.Entry<String, String> -> "-R" + e.key + "=" + e.value }
            .collect(Collectors.toMap(
                Function { obj: String -> obj }, Function { v: String -> "" })))
        return Params.of(
            "-en", name,
            "-er", root
        )
            .filter { p: Pair<String, Any?> -> p.value != null }
            .filter { p: Pair<String, Any> -> !p.value.toString().isEmpty() }
            .merge(Params.of(fieldsParams))
            .merge(collectionOptions.params)
    }

    override fun toString(): String {
        return params.withKVDelimiter(" ").formatAsLine().replace("\\s+".toRegex(), " ")
    }

    open class Builder {
        private var i = 1
        private val options = EntityOptions()
        fun name(name: String): Builder {
            options.name = name
            return this
        }

        fun root(root: String): Builder {
            options.root = root
            return this
        }

        fun css(css: String): Builder {
            return css("_" + i++, css)
        }

        fun css(vararg csss: String): Builder {
            for (css in csss) {
                css(css)
            }
            return this
        }

        fun css(name: String, css: String): Builder {
            options.cssRules[name] = css
            return this
        }

        fun xpath(xpath: String): Builder {
            return xpath("_" + i++, xpath)
        }

        fun xpath(vararg xpaths: String): Builder {
            for (xpath in xpaths) {
                xpath(xpath)
            }
            return this
        }

        fun xpath(name: String, xpath: String): Builder {
            options.xpathRules[name] = xpath
            return this
        }

        fun re(regex: String): Builder {
            return re("_" + i++, regex)
        }

        fun re(vararg regexes: String): Builder {
            for (regex in regexes) {
                re(regex)
            }
            return this
        }

        fun re(name: String, regex: String): Builder {
            options.regexRules[name] = regex
            return this
        }

        fun c_name(name: String): Builder {
            options.collectionOptions.name = name
            return this
        }

        fun c_root(css: String): Builder {
            options.collectionOptions.root = css
            return this
        }

        fun c_item(css: String): Builder {
            options.collectionOptions.item = css
            return this
        }

        fun c_css(css: String): Builder {
            return c_css("_" + i++, css)
        }

        fun c_css(vararg csss: String): Builder {
            for (css in csss) {
                c_css(css)
            }
            return this
        }

        fun c_css(name: String, css: String): Builder {
            options.collectionOptions.cssRules[name] = css
            return this
        }

        fun c_xpath(xpath: String): Builder {
            return c_xpath(xpath, xpath)
        }

        fun c_xpath(vararg xpaths: String): Builder {
            for (xpath in xpaths) {
                c_xpath(xpath)
            }
            return this
        }

        fun cxpath(name: String, xpath: String): Builder {
            options.collectionOptions.xpathRules.put(name, xpath)
            return this
        }

        fun c_re(regex: String): Builder {
            return c_re("_" + i++, regex)
        }

        fun c_re(vararg regexes: String): Builder {
            for (regex in regexes) {
                c_re(regex)
            }
            return this
        }

        fun c_re(name: String, regex: String): Builder {
            options.collectionOptions.regexRules.put(name, regex)
            return this
        }

        fun build(): EntityOptions {
            return options
        }

        fun <T : Builder?> `as`(o: T): T {
            if (o === this) return o
            throw ClassCastException()
        }
    }

    companion object {
        @JvmStatic
        fun parse(args: String): EntityOptions {
            val options = EntityOptions(args)
            options.parse()
            return options
        }

        fun newBuilder(): Builder {
            return Builder()
        }
    }
}