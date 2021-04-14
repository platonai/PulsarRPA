package ai.platon.pulsar.common.options.deprecated

import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.options.PulsarOptions
import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.Parameter
import org.apache.commons.lang3.tuple.Pair
import java.util.HashMap
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class CollectionOptions : PulsarOptions {
    @Parameter(names = ["-cn", "--collection-name"], description = "The name of the collection")
    var name: String = ""

    @Parameter(names = ["-cr", "--collection-root"], description = "The path of the collection")
    var root: String = ""

    @Parameter(names = ["-ci", "--collection-item"], description = "The path of entity fields")
    var item: String = ""

    @DynamicParameter(names = ["-FF"], description = "Pulsar field extractors to extract sub entity fields")
    var cssRules: MutableMap<String, String> = HashMap()

    @DynamicParameter(names = ["-XX"], description = "XPath selectors")
    var xpathRules: MutableMap<String, String> = HashMap()

    @DynamicParameter(names = ["-RR"], description = "Regex selectors")
    var regexRules: MutableMap<String, String> = HashMap()

    constructor() {}

    constructor(args: Array<String>) : super(args) {}

    fun hasRules(): Boolean {
        return !cssRules.isEmpty() || !xpathRules.isEmpty() || !regexRules.isEmpty()
    }

    override fun getParams(): Params {
        val fieldsParams = cssRules.entries.stream()
            .map { e: Map.Entry<String, String> -> "-FF" + e.key + "=" + e.value }
            .collect(Collectors.toMap(
                Function { obj: String -> obj }, Function<String, Any> { v: String? -> "" }))
        fieldsParams.putAll(xpathRules.entries.stream()
            .map { e: Map.Entry<String, String> -> "-XX" + e.key + "=" + e.value }
            .collect(Collectors.toMap(
                Function { obj: String -> obj }, Function { v: String? -> "" })))
        fieldsParams.putAll(regexRules.entries.stream()
            .map { e: Map.Entry<String, String> -> "-RR" + e.key + "=" + e.value }
            .collect(Collectors.toMap(
                Function { obj: String -> obj }, Function { v: String? -> "" })))
        return Params.of(
            "-cn", name,
            "-cr", root,
            "-ci", item
        )
            .filter { p: Pair<String?, Any?> -> p.value != null }
            .filter { p: Pair<String?, Any> -> !p.value.toString().isEmpty() }
            .merge(Params.of(fieldsParams))
    }

    override fun toString(): String {
        return params.withKVDelimiter(" ").formatAsLine().replace("\\s+".toRegex(), " ")
    }
}