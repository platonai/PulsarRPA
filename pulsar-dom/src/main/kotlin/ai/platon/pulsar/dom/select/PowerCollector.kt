package ai.platon.pulsar.dom.select

import ai.platon.pulsar.dom.nodes.node.ext.isAncestorOf
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.*

object PowerCollector {

    /**
     * Build a list of elements, by visiting root and every descendant of root, and testing it against the evaluator.
     * @param eval Evaluator to test elements against
     * @param root root of tree to descend
     * @return list of matches; empty if none
     */
    fun collect(eval: Evaluator, root: Element): Elements {
        val elements = Elements()
        NodeTraversor.traverse(Accumulator(root, elements, eval), root)
        return elements
    }

    fun findFirst(eval: Evaluator, root: Element): Element? {
        val finder = FirstFinder(root, eval)
        NodeTraversor.filter(finder, root)
        return finder.match
    }

    fun findFirstAccelerated(eval: Evaluator, root: Element): Element? {
        if (eval is CombiningEvaluator) {
            val first = eval.evaluators[0]
            if (first is Evaluator.Id) {
                val node = root.ownerDocument().variables[first.toString()]
                if (node is Element && root.isAncestorOf(node)) {
                    val eval0 = CombiningEvaluator.And(eval.evaluators.drop(0))
                    return findFirstAccelerated(eval0, node)
                }
            }
        }

        val finder = FirstFinder(root, eval)
        NodeTraversor.filter(finder, root)
        return finder.match
    }

    private class Accumulator(val root: Element, val elements: Elements, val eval: Evaluator): NodeVisitor {
        override fun head(node: Node, depth: Int) {
            if (node is Element) {
                if (eval.matches(root, node)) elements.add(node)
            }
        }

        override fun tail(node: Node, depth: Int) { // void
        }
    }

    private class FirstFinder(val root: Element, val eval: Evaluator): NodeFilter {
        var match: Element? = null
        override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
            if (node is Element) {
                if (eval.matches(root, node)) {
                    match = node
                    return NodeFilter.FilterResult.STOP
                }
            }
            return NodeFilter.FilterResult.CONTINUE
        }

        override fun tail(node: Node, depth: Int): NodeFilter.FilterResult {
            return NodeFilter.FilterResult.CONTINUE
        }
    }
}
