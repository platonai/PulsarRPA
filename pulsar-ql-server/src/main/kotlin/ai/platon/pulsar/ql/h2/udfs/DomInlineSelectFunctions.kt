package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.RegexExtractor
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.dom.nodes.A_LABELS
import ai.platon.pulsar.dom.select.first
import ai.platon.pulsar.dom.select.select2
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.Queries
import ai.platon.pulsar.ql.types.ValueDom
import com.google.common.base.Strings
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.h2.value.ValueArray
import org.h2.value.ValueString
import org.jsoup.nodes.Element

/**
 * Created by vincent on 17-11-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved.
 */
@Suppress("unused")
@UDFGroup(namespace = "DOM")
object DomInlineSelectFunctions {

    @UDFunction(description = "Select all match elements by the given css query from a DOM and return the result as an array of DOMs")
    @JvmStatic
    fun inlineSelect(dom: ValueDom, cssQuery: String): ValueArray {
        val elements = dom.element.select2(cssQuery)
        return Queries.toValueArray(elements)
    }

    @UDFunction(description = "Select all match elements by the given css query from a DOM and return the result as an array of DOMs")
    @JvmStatic
    fun inlineSelect(dom: ValueDom, cssQuery: String, offset: Int, limit: Int): ValueArray {
        val elements = dom.element.select2(cssQuery, offset, limit)
        return Queries.toValueArray(elements)
    }

    @UDFunction(description = "Select all match elements by the given css query from a DOM and return the result as an array of DOMs")
    @JvmStatic
    fun inlineSelectText(dom: ValueDom, cssQuery: String): ValueArray {
        return inlineSelectText(dom, cssQuery, 1, 40)
    }

    @UDFunction(description = "Select all match elements by the given css query from a DOM and return the result as an array of DOMs")
    @JvmStatic
    fun inlineSelectText(dom: ValueDom, cssQuery: String, offset: Int, limit: Int): ValueArray {
        val texts = dom.element.select2(cssQuery, offset, limit).map { ValueString.get(it.text()) }.toTypedArray()
        return ValueArray.get(texts)
    }
}
