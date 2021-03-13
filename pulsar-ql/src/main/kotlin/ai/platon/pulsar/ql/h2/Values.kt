package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.ql.types.ValueDom
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
