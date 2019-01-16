package `fun`.platonic.pulsar.ql.h2

import `fun`.platonic.pulsar.ql.types.ValueDom
import `fun`.platonic.pulsar.dom.FeaturedDocument
import org.jsoup.nodes.Element

fun domValue(document: FeaturedDocument): ValueDom {
    return ValueDom.get(document.unbox())
}

fun domValue(ele: Element): ValueDom {
    return ValueDom.get(ele)
}

fun docValue(dom: ValueDom): FeaturedDocument {
    return FeaturedDocument(dom.getElement().ownerDocument())
}
