package ai.platon.pulsar.dom.select

import ai.platon.pulsar.dom.FeaturedDocument
import org.apache.commons.lang3.math.NumberUtils
import org.jsoup.helper.Validate
import org.jsoup.internal.Normalizer.normalize
import org.jsoup.internal.StringUtil
import org.jsoup.parser.TokenQueue
import org.jsoup.select.Evaluator
import org.jsoup.select.Evaluator.*
import java.util.regex.Pattern

/**
 * Parses a CSS selector into an Evaluator tree.
 */
class PowerQueryParser(private val query: String) {
    private val tq: TokenQueue = TokenQueue(query)
    private val evals: MutableList<Evaluator> = ArrayList()
    /**
     * Parse the query
     * @return Evaluator
     */
    fun parse(): Evaluator {
        tq.consumeWhitespace()
        if (tq.matchesAny(*combinators)) { // if starts with a combinator, use root as elements
            evals.add(StructuralEvaluator.Root())
            combinator(tq.consume())
        } else {
            findElements()
        }
        while (!tq.isEmpty) { // hierarchy and extras
            val seenWhite = tq.consumeWhitespace()
            if (tq.matchesAny(*combinators)) {
                combinator(tq.consume())
            } else if (seenWhite) {
                combinator(' ')
            } else { // E.class, E#id, E[attr] etc. AND
                findElements() // take next el, #. etc off queue
            }
        }
        return if (evals.size == 1) evals[0] else CombiningEvaluator.And(evals)
    }

    private fun combinator(combinator: Char) {
        tq.consumeWhitespace()
        val subQuery = consumeSubQuery() // support multi > childs
        var rootEval: Evaluator // the new topmost evaluator
        var currentEval: Evaluator // the evaluator the new eval will be combined to. could be root, or rightmost or.
        val newEval = parse(subQuery) // the evaluator to add into target evaluator
        var replaceRightMost = false
        if (evals.size == 1) {
            currentEval = evals[0]
            rootEval = currentEval
            // make sure OR (,) has precedence:
            if (rootEval is CombiningEvaluator.Or && combinator != ',') {
                currentEval = (currentEval as CombiningEvaluator.Or).rightMostEvaluator()!!
                replaceRightMost = true
            }
        } else {
            currentEval = CombiningEvaluator.And(evals)
            rootEval = currentEval
        }
        evals.clear()
        // for most combinators: change the current eval into an AND of the current eval and the new eval
        when (combinator) {
            '>' -> currentEval = CombiningEvaluator.And(newEval, StructuralEvaluator.ImmediateParent(currentEval))
            ' ' -> currentEval = CombiningEvaluator.And(newEval, StructuralEvaluator.Parent(currentEval))
            '+' -> currentEval = CombiningEvaluator.And(newEval, StructuralEvaluator.ImmediatePreviousSibling(currentEval))
            '~' -> currentEval = CombiningEvaluator.And(newEval, StructuralEvaluator.PreviousSibling(currentEval))
            ',' -> { // group or.
                val or: CombiningEvaluator.Or
                if (currentEval is CombiningEvaluator.Or) {
                    or = currentEval
                    or.add(newEval)
                } else {
                    or = CombiningEvaluator.Or()
                    or.add(currentEval)
                    or.add(newEval)
                }
                currentEval = or
            }
            else -> throw PowerSelectorParseException("Unknown combinator: $combinator")
        }
        if (replaceRightMost) (rootEval as CombiningEvaluator.Or).replaceRightMostEvaluator(currentEval) else rootEval = currentEval
        evals.add(rootEval)
    }

    private fun consumeSubQuery(): String {
        val sq = StringUtil.borrowBuilder()
        while (!tq.isEmpty) {
            if (tq.matches("("))
                sq.append("(").append(tq.chompBalanced('(', ')')).append(")")
            else if (tq.matches("["))
                sq.append("[").append(tq.chompBalanced('[', ']')).append("]")
            else if (tq.matchesAny(*combinators))
                break
            else
                sq.append(tq.consume())
        }
        return StringUtil.releaseBuilder(sq)
    }

    private fun findElements() {
        if (tq.matchChomp("#"))
            byId()
        else if (tq.matchChomp("."))
            byClass()
        else if (tq.matchesWord() || tq.matches("*|"))
            byTag()
        else if (tq.matches("["))
            byAttribute()
        else if (tq.matchChomp("*"))
            allElements()
        else if (tq.matchChomp(":lt("))
            indexLessThan()
        else if (tq.matchChomp(":gt("))
            indexGreaterThan()
        else if (tq.matchChomp(":eq("))
            indexEquals()
        else if (tq.matches(":has("))
            has()
        else if (tq.matches(":contains("))
            contains(false)
        else if (tq.matches(":containsOwn("))
            contains(true)
        else if (tq.matches(":containsData("))
            containsData()
        else if (tq.matches(":matches("))
            matches(false)
        else if (tq.matches(":matchesOwn("))
            matches(true)
        else if (tq.matches(":not("))
            not()
        else if (tq.matchChomp(":in-box("))
            byBox2()
        else if (tq.matchChomp(":expr("))
            byExpression()
        else if (tq.matchChomp(":nth-child("))
            cssNthChild(false, false)
        else if (tq.matchChomp(":nth-last-child("))
            cssNthChild(true, false)
        else if (tq.matchChomp(":nth-of-type("))
            cssNthChild(false, true)
        else if (tq.matchChomp(":nth-last-of-type("))
            cssNthChild(true, true)
        else if (tq.matchChomp(":first-child"))
            evals.add(IsFirstChild())
        else if (tq.matchChomp(":last-child"))
            evals.add(IsLastChild())
        else if (tq.matchChomp(":first-of-type"))
            evals.add(IsFirstOfType())
        else if (tq.matchChomp(":last-of-type"))
            evals.add(IsLastOfType())
        else if (tq.matchChomp(":only-child"))
            evals.add(IsOnlyChild())
        else if (tq.matchChomp(":only-of-type"))
            evals.add(IsOnlyOfType())
        else if (tq.matchChomp(":empty"))
            evals.add(IsEmpty())
        else if (tq.matchChomp(":root"))
            evals.add(IsRoot())
        else if (tq.matchChomp(":matchText"))
            evals.add(MatchText())
        else
            throw PowerSelectorParseException(
                    "Could not parse query '%s': unexpected token at '%s'", query, tq.remainder())
    }

    private fun byId() {
        val id = tq.consumeCssIdentifier()
        Validate.notEmpty(id)
        evals.add(Id(id))
    }

    private fun byClass() {
        val className = tq.consumeCssIdentifier()
        Validate.notEmpty(className)
        evals.add(Class(className.trim { it <= ' ' }))
    }

    private fun byTag() {
        var tagName = tq.consumeElementSelector()
        Validate.notEmpty(tagName)
        // namespaces: wildcard match equals(tagName) or ending in ":"+tagName
        if (tagName.startsWith("*|")) {
            evals.add(CombiningEvaluator.Or(Tag(normalize(tagName)), TagEndsWith(normalize(tagName.replace("*|", ":")))))
        } else { // namespaces: if element name is "abc:def", selector must be "abc|def", so flip:
            if (tagName.contains("|")) tagName = tagName.replace("|", ":")
            evals.add(Tag(tagName.trim { it <= ' ' }))
        }
    }

    private fun byAttribute() {
        val cq = TokenQueue(tq.chompBalanced('[', ']')) // content queue
        val key = cq.consumeToAny(*AttributeEvals) // eq, not, start, end, contain, match, (no val)
        Validate.notEmpty(key)
        cq.consumeWhitespace()
        if (cq.isEmpty) {
            when {
                key.startsWith("^") -> evals.add(AttributeStarting(key.substring(1)))
                else -> evals.add(Attribute(key))
            }
        } else {
            when {
                cq.matchChomp("=") -> evals.add(AttributeWithValue(key, cq.remainder()))
                cq.matchChomp("!=") -> evals.add(AttributeWithValueNot(key, cq.remainder()))
                cq.matchChomp("^=") -> evals.add(AttributeWithValueStarting(key, cq.remainder()))
                cq.matchChomp("$=") -> evals.add(AttributeWithValueEnding(key, cq.remainder()))
                cq.matchChomp("*=") -> evals.add(AttributeWithValueContaining(key, cq.remainder()))
                cq.matchChomp("~=") -> evals.add(AttributeWithValueMatching(key, Pattern.compile(cq.remainder())))
                else -> throw PowerSelectorParseException("Could not parse attribute query '%s': unexpected token at '%s'", query, cq.remainder())
            }
        }
    }

    private fun byExpression() {
        val s = normalize(tq.chompTo(")"))
        evals.add(PowerEvaluator.ByExpression(s))
    }

    private fun allElements() {
        evals.add(AllElements())
    }

    // pseudo selectors :lt, :gt, :eq
    private fun indexLessThan() {
        evals.add(IndexLessThan(consumeIndex()))
    }

    private fun indexGreaterThan() {
        evals.add(IndexGreaterThan(consumeIndex()))
    }

    private fun indexEquals() {
        evals.add(IndexEquals(consumeIndex()))
    }

    private fun cssNthChild(backwards: Boolean, ofType: Boolean) {
        val argS = normalize(tq.chompTo(")"))
        val mAB = NTH_AB.matcher(argS)
        val mB = NTH_B.matcher(argS)
        val a: Int
        val b: Int
        when {
            "odd" == argS -> {
                a = 2
                b = 1
            }
            "even" == argS -> {
                a = 2
                b = 0
            }
            mAB.matches() -> {
                a = if (mAB.group(3) != null) mAB.group(1).replaceFirst("^\\+".toRegex(), "").toInt() else 1
                b = if (mAB.group(4) != null) mAB.group(4).replaceFirst("^\\+".toRegex(), "").toInt() else 0
            }
            mB.matches() -> {
                a = 0
                b = mB.group().replaceFirst("^\\+".toRegex(), "").toInt()
            }
            else -> {
                throw PowerSelectorParseException("Could not parse nth-index '%s': unexpected format", argS)
            }
        }
        if (ofType) if (backwards) evals.add(IsNthLastOfType(a, b)) else evals.add(IsNthOfType(a, b)) else {
            if (backwards) evals.add(IsNthLastChild(a, b)) else evals.add(IsNthChild(a, b))
        }
    }

    private fun consumeIndex(): Int {
        val indexS = tq.chompTo(")").trim { it <= ' ' }
        Validate.isTrue(StringUtil.isNumeric(indexS), "Index must be numeric")
        return indexS.toInt()
    }

    // pseudo selector :has(el)
    private fun has() {
        tq.consume(":has")
        val subQuery = tq.chompBalanced('(', ')')
        Validate.notEmpty(subQuery, ":has(el) subselect must not be empty")
        evals.add(StructuralEvaluator.Has(parse(subQuery)))
    }

    // pseudo selector :contains(text), containsOwn(text)
    private fun contains(own: Boolean) {
        tq.consume(if (own) ":containsOwn" else ":contains")
        val searchText = TokenQueue.unescape(tq.chompBalanced('(', ')'))
        Validate.notEmpty(searchText, ":contains(text) query must not be empty")
        if (own) evals.add(ContainsOwnText(searchText)) else evals.add(ContainsText(searchText))
    }

    // pseudo selector :containsData(data)
    private fun containsData() {
        tq.consume(":containsData")
        val searchText = TokenQueue.unescape(tq.chompBalanced('(', ')'))
        Validate.notEmpty(searchText, ":containsData(text) query must not be empty")
        evals.add(ContainsData(searchText))
    }

    // :matches(regex), matchesOwn(regex)
    private fun matches(own: Boolean) {
        tq.consume(if (own) ":matchesOwn" else ":matches")
        val regex = tq.chompBalanced('(', ')') // don't unescape, as regex bits will be escaped
        Validate.notEmpty(regex, ":matches(regex) query must not be empty")
        if (own) evals.add(MatchesOwn(Pattern.compile(regex))) else evals.add(Matches(Pattern.compile(regex)))
    }

    // :not(selector)
    private operator fun not() {
        tq.consume(":not")
        val subQuery = tq.chompBalanced('(', ')')
        Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty")
        evals.add(StructuralEvaluator.Not(parse(subQuery)))
    }

    /**
     * There are four formats to indicate a rectangle:
     *
     * 1. :in-box(width, height)
     * 2. :in-box(width, height, deviation)
     * 3. :in-box(top, left, width, height)
     * 4. :in-box(top, left, width, height, deviation)
     *
     * and each value can be '*' to match all size
     */
    private fun byBox1() {

        val s = tq.remainder()

        val args = normalize(s)
                .split('x', 'X')
                .joinToString { "${it[0]}, ${it[1]}" }

        byBox(args)
    }

    private fun byBox2() {
        val argS = normalize(tq.chompTo(")")).replace("\\s+".toRegex(), "")
        byBox(argS)
    }

    private fun byBox(expr: String) {
        var argS = expr

        // If rect x in rect R, then X must satisfy:
        // x.top >= R.top && x.left >= R.left && x.width <= R.width && x.height <= R.height
        val ops = arrayOf(">=", ">=", "<=", "<=")
        val operands = IntArray(4)
        var allowError = FeaturedDocument.SELECTOR_IN_BOX_DEVIATION

        if (PATTERN_FLOAT_RECT.matcher(argS).matches()) {
            argS = "*,*,$argS"
        }

        val matcher = PATTERN_RECT.matcher(argS)
        if (matcher.find()) {
            for (i in ops.indices) {
                val sign = matcher.group(2 * i + 1)
                val operand = matcher.group(2 * i + 2)

                if (operand == "*") {
                    ops[i] = "*"
                } else {
                    operands[i] = NumberUtils.toInt(operand)
                    if ("-" == sign) {
                        operands[i] = -operands[i]
                    }
                }
            }

            val s = matcher.group(9)
            if (s != null) {
                allowError = NumberUtils.toInt(s.replaceFirst(",".toRegex(), ""))
            }
        } else {
            throw PowerSelectorParseException("Could not parse in-box '%s': unexpected format", argS)
        }

        evals.add(PowerEvaluator.ByBox(ops, operands, allowError))
    }

    companion object {
        private val combinators = arrayOf(",", ">", "+", "~", " ")
        private val AttributeEvals = arrayOf("=", "!=", "^=", "$=", "*=", "~=")
        /**
         * Parse a CSS query into an Evaluator.
         * @param query CSS query
         * @return Evaluator
         */
        fun parse(query: String): Evaluator {
            return try {
                val p = PowerQueryParser(query)
                p.parse()
            } catch (e: IllegalArgumentException) {
                throw PowerSelectorParseException(e.message ?: "Unknown IllegalArgumentException")
            }
        }

        //pseudo selectors :first-child, :last-child, :nth-child, ...
        private val NTH_AB = Pattern.compile("(([+-])?(\\d+)?)n(\\s*([+-])?\\s*\\d+)?", Pattern.CASE_INSENSITIVE)
        private val NTH_B = Pattern.compile("([+-])?(\\d+)")

        //pseudo selector :in-box
        const val REGEX_RECT = "([+-])?(\\*|\\d+),([+-])?(\\*|\\d+),([+-])?(\\*|\\d+),([+-])?(\\*|\\d+)(,\\d+)?"
        val PATTERN_RECT = Pattern.compile(REGEX_RECT, Pattern.CASE_INSENSITIVE)

        const val REGEX_FLOAT_RECT = "([+-])?(\\*|\\d+),([+-])?(\\*|\\d+)(,\\d+)?"
        val PATTERN_FLOAT_RECT = Pattern.compile(REGEX_FLOAT_RECT, Pattern.CASE_INSENSITIVE)
    }
}
