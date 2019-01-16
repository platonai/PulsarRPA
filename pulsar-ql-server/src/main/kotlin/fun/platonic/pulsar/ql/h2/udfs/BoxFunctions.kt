package `fun`.platonic.pulsar.ql.h2.udfs

import `fun`.platonic.pulsar.dom.nodes.node.ext.convertBox
import `fun`.platonic.pulsar.ql.annotation.UDFGroup
import `fun`.platonic.pulsar.ql.annotation.UDFunction
import `fun`.platonic.pulsar.ql.types.ValueDom
import org.h2.value.ValueArray

/**
 * Created by vincent on 18-02-06.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
@Suppress("unused")
@UDFGroup(namespace = "IN_BOX")
object BoxFunctions {

    @JvmStatic
    @UDFunction
    fun all(dom: ValueDom, box: String): ValueArray {
        return DomSelectFunctions.inlineSelect(dom, convertBox(box))
    }

    @JvmStatic
    @UDFunction
    fun all(dom: ValueDom, box: String, offset: Int, limit: Int): ValueArray {
        return DomSelectFunctions.inlineSelect(dom, convertBox(box), offset, limit)
    }

    @JvmStatic
    @UDFunction
    fun first(dom: ValueDom, box: String): ValueDom {
        return DomSelectFunctions.selectFirst(dom, convertBox(box))
    }

    @JvmStatic
    @UDFunction
    fun nth(dom: ValueDom, box: String, n: Int): ValueDom {
        return DomSelectFunctions.selectNth(dom, convertBox(box), n)
    }

    @JvmStatic
    @UDFunction
    fun firstText(dom: ValueDom, box: String): String {
        return DomSelectFunctions.firstText(dom, convertBox(box))
    }

    @JvmStatic
    @UDFunction
    fun nthText(dom: ValueDom, box: String, n: Int): String {
        return DomSelectFunctions.nthText(dom, convertBox(box), n)
    }

    @JvmStatic
    @UDFunction
    fun firstImg(dom: ValueDom, box: String): String {
        return DomSelectFunctions.firstImg(dom, convertBox(box))
    }

    @JvmStatic
    @UDFunction
    fun nthImg(dom: ValueDom, box: String, n: Int): String {
        return DomSelectFunctions.nthImg(dom, convertBox(box), n)
    }

    @JvmStatic
    @UDFunction
    fun firstHref(dom: ValueDom, box: String): String {
        return DomSelectFunctions.firstHref(dom, convertBox(box))
    }

    @JvmStatic
    @UDFunction
    fun nthHref(dom: ValueDom, box: String, n: Int): String {
        return DomSelectFunctions.nthHref(dom, convertBox(box), n)
    }

    @JvmStatic
    @UDFunction
    fun firstRe1(dom: ValueDom, box: String, regex: String): String {
        return DomSelectFunctions.firstRe1(dom, convertBox(box), regex)
    }

    @JvmStatic
    @UDFunction
    fun firstRe1(dom: ValueDom, box: String, regex: String, group: Int): String {
        return DomSelectFunctions.firstRe1(dom, convertBox(box), regex, group)
    }

    @JvmStatic
    @UDFunction
    fun firstRe2(dom: ValueDom, box: String, regex: String): ValueArray {
        return DomSelectFunctions.firstRe2(dom, convertBox(box), regex)
    }

    @JvmStatic
    @UDFunction
    fun firstRe2(dom: ValueDom, box: String, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        return DomSelectFunctions.firstRe2(dom, convertBox(box), regex, keyGroup, valueGroup)
    }
}
