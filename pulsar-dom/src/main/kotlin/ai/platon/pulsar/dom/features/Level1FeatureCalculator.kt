package ai.platon.pulsar.dom.features

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.common.math.vectors.set
import ai.platon.pulsar.dom.features.defined.*
import ai.platon.pulsar.dom.nodes.DOMRect
import ai.platon.pulsar.dom.nodes.forEachElement
import ai.platon.pulsar.dom.nodes.node.ext.*
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

/**
 * The level 1 feature calculator calculate for the minimal features
 * */
class Level1FeatureCalculator : AbstractFeatureCalculator() {
    companion object {
        init {
            ResourceLoader.addClassFactory(ClassFactory())
            if (FeatureRegistry.registeredFeatures.isEmpty()) {
                FeatureRegistry.register(F.entries.map { it.toFeature() })
                require(FeatureRegistry.registeredFeatures.size == N)
            }
        }
    }

    override fun calculate(document: Document) {
        NodeTraversor.traverse(Level1NodeFeatureCalculatorVisitor(), document)
    }

    override fun dispose() {
        FeatureRegistry.unregister()
    }
}

/**
 * The class factory for ResourceLoader
 * */
class ClassFactory : ResourceLoader.ClassFactory {
    override fun match(name: String): Boolean {
        return name.startsWith(this.javaClass.`package`.name)
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String): Class<*> {
        return this.javaClass.classLoader.loadClass(name)
    }
}

private class Level1NodeFeatureCalculatorVisitor : NodeVisitor {
    var sequence: Int = 0
        private set

    // hit when the node is first seen
    override fun head(node: Node, depth: Int) {
        val extension = node.extension
        extension.features = ArrayRealVector(FeatureRegistry.registeredFeatures.size)

        extension.features[DEP] = depth.toDouble()
        extension.features[SEQ] = sequence.toDouble()

        calcSelfIndicator(node)
        ++sequence
    }

    private fun calcSelfIndicator(node: Node) {
        if (node !is Element && node !is TextNode) {
            return
        }

        val extension = node.extension
        val rect = getDOMRect(node)
        if (!rect.isEmpty) {
            extension.features[TOP] = rect.top
            extension.features[LEFT] = rect.left
            extension.features[WIDTH] = rect.width
            extension.features[HEIGHT] = rect.height
        }

        if (node is TextNode) {
            // Trim: remove all surrounding unicode white spaces, including all HT, VT, LF, FF, CR, ASCII space, etc
            // @see https://en.wikipedia.org/wiki/Whitespace_character
            extension.immutableText = node.text()
            val text = extension.immutableText
            val ch = text.length.toDouble()

            if (ch > 0) {
                accumulateFeatures(node, FeatureEntry(CH, ch))
            }
        }

        if (node is Element) {
            var a = 0.0
            var va = 0.0
            var aW = 0.0
            var aH = 0.0

            var img = 0.0
            var vimg = 0.0
            var imgW = 0.0
            var imgH = 0.0

            if (node.nodeName() == "a") {
                ++a
                aW = rect.width
                aH = rect.height
                if (aW > 0 && aH > 0) {
                    ++va
                }
            }

            if (node.nodeName() == "img") {
                ++img
                imgW = rect.width
                imgH = rect.height
                if (imgW > 0 && imgH > 0) {
                    ++vimg
                }
            }

            accumulateFeatures(
                node,
                FeatureEntry(A, a),
                FeatureEntry(IMG, img)
            )
        }
    }

    override fun tail(node: Node, depth: Int) {
        if (node !is Element && node !is TextNode) {
            return
        }

        if (node is TextNode) {
            val parent = node.parent()!!
            val extension = node.extension

            // no-blank own text node
            val otn = if (extension.features[CH] == 0.0) 0.0 else 1.0
            val votn = if (otn > 0 && extension.features[WIDTH] > 0 && extension.features[HEIGHT] > 0) 1.0 else 0.0
            accumulateFeatures(
                parent,
                FeatureEntry(TN, otn),
                node.getFeatureEntry(CH)
            )

            return
        }

        if (node is Element) {
            val pe = node.parent() ?: return

            accumulateFeatures(
                pe,
                node.getFeatureEntry(CH),
                node.getFeatureEntry(TN),
                node.getFeatureEntry(A),
                node.getFeatureEntry(IMG),
                FeatureEntry(C, 1.0)
            )

            node.childNodes().forEach {
                if (it is Element) {
                    it.extension.features[SIB] = node.extension.features[C]
                }
            }
        }

        if (node.nodeName().equals("body", ignoreCase = true)) {
            val rect = calculateBodyRect(node)
            node.width = rect.width.toInt()
            node.height = rect.height.toInt()
        }
    }

    private fun accumulateFeatures(node: Node, vararg features: FeatureEntry) {
        for (feature in features) {
            val old = node.getFeature(feature.key)
            node.setFeature(feature.key, feature.value + old)
        }
    }

    private fun getDOMRect(node: Node): DOMRect {
        return if (node is TextNode) getDOMRectInternal("tv", node)
        else DOMRect.parseDOMRect(node.attr("vi"))
    }

    private fun getDOMRectInternal(attrKey: String, node: TextNode): DOMRect {
        val parent = node.parent()!!
        val i = node.siblingIndex()
        val vi = parent.attr("$attrKey$i")
        return DOMRect.parseDOMRect(vi)
    }

    private fun calculateBodyRect(body: Node): DOMRect {
        val minW = 900.0
        val widths = DescriptiveStatistics()
        widths.addValue(minW)
        var height = body.height

        body.forEachElement {
            if (it.width > minW) {
                widths.addValue(it.width.toDouble())
            }
            if (it.y2 > height) {
                height = it.y2
            }
        }

        return DOMRect(0.0, 0.0, widths.getPercentile(90.0), 20 + height.toDouble())
    }

    private fun divide(ele: Element, numerator: Int, denominator: Int, divideByZeroValue: Double): Double {
        val n = ele.getFeature(numerator)
        val d = ele.getFeature(denominator)

        return if (d == 0.0) divideByZeroValue else n / d
    }
}
