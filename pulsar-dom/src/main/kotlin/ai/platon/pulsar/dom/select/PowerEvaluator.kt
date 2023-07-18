package ai.platon.pulsar.dom.select

import ai.platon.pulsar.dom.features.FeatureRegistry
import ai.platon.pulsar.dom.features.defined.HEIGHT
import ai.platon.pulsar.dom.features.defined.LEFT
import ai.platon.pulsar.dom.features.defined.TOP
import ai.platon.pulsar.dom.features.defined.WIDTH
import ai.platon.pulsar.dom.nodes.node.ext.getFeature
import com.udojava.evalex.Expression
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import java.math.BigDecimal

internal abstract class PowerEvaluator : Evaluator() {

    companion object {
        /**
         * Handle non-standard CSS identifier. Some websites use selectors what do not match the standard. For example,
         *
         * <code>
         *     <div class='KAHaP+'></div>
         * </code>
         *
         * the charactor "+" is not allowed in a class name so Jsoup throws a SelectorParseException,
         * and pulsar-dom throws a PowerSelectorParseException.
         *
         * Jsoup follows the CSS2 value defination standard: https://www.w3.org/TR/CSS2/syndata.html#value-def-identifier
         *
         * <p>
         * In CSS, identifiers (including element names, classes, and IDs in
         * [selectors](https://www.w3.org/TR/CSS2/selector.html)) can contain only the characters [a-zA-Z0-9]
         * and ISO 10646 characters U+00A0 and higher, plus the hyphen (-) and the underscore (_);
         * they cannot start with a digit, two hyphens, or a hyphen followed by a digit.
         * Identifiers can also contain escaped characters and any ISO 10646 character as a numeric code.
         * For instance, the identifier "B&W?" may be written as "B\&W\?" or "B\26 W\3F".
         *
         * For more about valid characters in a CSS selector:
         * https://pineco.de/css-quick-tip-the-valid-characters-in-a-custom-css-selector/
         * A selector will look something like this:
         * -?[_a-zA-Z]+[_-a-zA-Z0-9]*
         *
         * @see [Issue #10](https://github.com/platonai/pulsar/issues/10)
         * */
        fun encodeQuery(query: String): String {
            // correct non-standard identifier
            // <div class='KAHaP+'></div>
            // to be:
            // <div class='KAHaP--x--'></div>
            // TODO: can we escape these characters? we need a test
            var q = query.replace("+", "--x--")

            // Further fix: adjacent sibling selector (+), for example "div + p".
            q = q.replace(" --x-- ", " + ")
            return q
        }

        fun decodeQuery(query: String): String {
            var decoded = query
            if (query.contains("--x--")) {
                decoded = decoded.replace("--x--", "+")
            }
            return decoded
        }
    }

    /**
     * Evaluator for element id
     */
    class PowerId(private val id: String) : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return decodeQuery(id) == element.id()
        }

        override fun toString(): String {
            return String.format("#%s", id)
        }
    }

    /**
     * Evaluator for element class
     */
    class PowerClass(private val className: String) : Evaluator() {
        override fun matches(root: Element, element: Element): Boolean {
            return element.hasClass(decodeQuery(className))
        }

        override fun toString(): String {
            return String.format(".%s", className)
        }
    }

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
            for (name in FeatureRegistry.featureNames) {
                // the prefix "_" is compatible with Web SQL, may remove in later versions
                val v = element.getFeature(name)
                if (v.isNaN()) {
                    return false
                }
                val value = BigDecimal(v)
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
