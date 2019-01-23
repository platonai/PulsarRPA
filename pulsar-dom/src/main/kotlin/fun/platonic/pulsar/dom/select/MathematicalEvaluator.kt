package `fun`.platonic.pulsar.dom.select

import `fun`.platonic.pulsar.dom.features.HEIGHT
import `fun`.platonic.pulsar.dom.features.LEFT
import `fun`.platonic.pulsar.dom.features.NodeFeature.Companion.featureNames
import `fun`.platonic.pulsar.dom.features.TOP
import `fun`.platonic.pulsar.dom.features.WIDTH
import `fun`.platonic.pulsar.dom.nodes.node.ext.getFeature
import com.udojava.evalex.Expression
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import java.math.BigDecimal

internal abstract class MathematicalEvaluator : Evaluator() {

    class ByBox(
            private val ops: Array<String>,
            private val restriction: IntArray,
            private val allowError: Int) : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            val p = element.parent()
            if (p == null || p is Document) {
                return false
            }

            val leftOperands = DoubleArray(4)
            leftOperands[0] = element.getFeature(TOP)
            leftOperands[1] = element.getFeature(LEFT)
            leftOperands[2] = element.getFeature(WIDTH)
            leftOperands[3] = element.getFeature(HEIGHT)

            var satisfied = false
            for (i in leftOperands.indices) {
                val op = ops[i]
                val x = leftOperands[i]
                val restrict = restriction[i].toDouble()

                var error = Math.abs(x - restrict)
                when (op) {
                    "<" -> satisfied = x < restrict
                    "<=" -> satisfied = x <= restrict
                    ">" -> satisfied = x > restrict
                    ">=" -> satisfied = x >= restrict
                    "*" -> {
                        satisfied = true
                        error = 0.0
                    }
                    else -> satisfied = false
                }

                // satisfied, but there is two much gaps between our test box and the target box
                if (satisfied && error > allowError) {
                    satisfied = false
                }

                if (!satisfied) {
                    break
                }
            }

            return satisfied
        }

        override fun toString(): String {
            return String.format("in-box(%s%d, %s%d, %s%d, %s%d, %d)",
                    ops[0], restriction[0],
                    ops[1], restriction[1],
                    ops[2], restriction[2],
                    ops[3], restriction[3],
                    allowError)
        }
    }

    /**
     * Evaluate simple mathematical and boolean expressions.
     * @see [EvalEx](https://github.com/uklimaschewski/EvalEx)
     */
    class ByExpression(private val expr: String) : Evaluator() {
        private val expression = Expression(expr)

        override fun matches(root: Element, element: Element): Boolean {
            for (name in featureNames) {
                // the prefix "_" is compatible with Web SQL, may remove in later versions
                val value = BigDecimal(element.getFeature(name))
                if (expr.contains("_$name")) {
                    expression.setVariable("_$name", value)
                } else if (expr.contains(name)) {
                    expression.setVariable(name, value)
                }
            }

            return expression.isBoolean && "1" == expression.eval().toString()
        }

        override fun toString(): String {
            return String.format("[%s]", expr)
        }
    }
}
