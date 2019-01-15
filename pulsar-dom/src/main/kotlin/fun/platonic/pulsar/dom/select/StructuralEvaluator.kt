package `fun`.platonic.pulsar.dom.select

import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator

/**
 * Base structural evaluator.
 */
internal abstract class StructuralEvaluator : Evaluator() {

    internal class Root : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return root === element
        }
    }

    internal class Has(val evaluator: Evaluator) : StructuralEvaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            for (e in element.allElements) {
                if (e !== element && evaluator.matches(root, e))
                    return true
            }
            return false
        }

        override fun toString(): String {
            return String.format(":has(%s)", evaluator)
        }
    }

    internal class Not(val evaluator: Evaluator) : StructuralEvaluator() {
        override fun matches(root: Element, node: Element): Boolean {
            return !evaluator.matches(root, node)
        }

        override fun toString(): String {
            return String.format(":not%s", evaluator)
        }
    }

    internal class Parent(val evaluator: Evaluator) : StructuralEvaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            if (root === element)
                return false

            var parent = element.parent()
            while (true) {
                if (evaluator.matches(root, parent))
                    return true
                if (parent === root)
                    break
                parent = parent.parent()
            }
            return false
        }

        override fun toString(): String {
            return String.format(":parent%s", evaluator)
        }
    }

    internal class ImmediateParent(val evaluator: Evaluator) : StructuralEvaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            if (root === element)
                return false

            val parent = element.parent()
            return parent != null && evaluator.matches(root, parent)
        }

        override fun toString(): String {
            return String.format(":ImmediateParent%s", evaluator)
        }
    }

    internal class PreviousSibling(val evaluator: Evaluator) : StructuralEvaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            if (root === element)
                return false

            var prev: Element? = element.previousElementSibling()

            while (prev != null) {
                if (evaluator.matches(root, prev))
                    return true

                prev = prev.previousElementSibling()
            }
            return false
        }

        override fun toString(): String {
            return String.format(":prev*%s", evaluator)
        }
    }

    internal class ImmediatePreviousSibling(val evaluator: Evaluator) : StructuralEvaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            if (root === element)
                return false

            val prev = element.previousElementSibling()
            return prev != null && evaluator.matches(root, prev)
        }

        override fun toString(): String {
            return String.format(":prev%s", evaluator)
        }
    }
}
