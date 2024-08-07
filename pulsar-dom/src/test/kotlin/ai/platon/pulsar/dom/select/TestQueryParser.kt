package ai.platon.pulsar.dom.select

import org.jsoup.select.Evaluator
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the Selector Query Parser.
 */
class TestQueryParser {

    @Test
    fun testOrGetsCorrectPrecedence() {
        // tests that a selector "a b, c d, e f" evals to (a AND b) OR (c AND d) OR (e AND f)"
        // top level or, three child ands
        val eval = PowerQueryParser.parse("a b, c d, e f")
        assertTrue(eval is CombiningEvaluator.Or)
        val or = eval as CombiningEvaluator.Or
        assertEquals(3, or.evaluators.size.toLong())
        for (innerEval in or.evaluators) {
            assertTrue(innerEval is CombiningEvaluator.And)
            val and = innerEval as CombiningEvaluator.And
            assertEquals(2, and.evaluators.size.toLong())
            assertTrue(and.evaluators[0] is Evaluator.Tag)
            assertTrue(and.evaluators[1] is StructuralEvaluator.Parent)
        }
    }

    @Test
    fun testParsesMultiCorrectly() {
        val eval = PowerQueryParser.parse(".foo > ol, ol > li + li")
        assertTrue(eval is CombiningEvaluator.Or)
        val or = eval as CombiningEvaluator.Or
        assertEquals(2, or.evaluators.size.toLong())

        val andLeft = or.evaluators[0] as CombiningEvaluator.And
        val andRight = or.evaluators[1] as CombiningEvaluator.And

        assertEquals("ol :ImmediateParent.foo", andLeft.toString())
        assertEquals(2, andLeft.evaluators.size.toLong())
        assertEquals("li :prevli :ImmediateParentol", andRight.toString())
        assertEquals(2, andLeft.evaluators.size.toLong())
    }

    @Test
    fun exceptionOnUncloseAttribute() {
        assertThrows<PowerSelectorParseException> {
            val parse = PowerQueryParser.parse("section > a[href=\"]")
        }
    }

    @Test
    fun testParsesSingleQuoteInContains() {
        assertThrows<PowerSelectorParseException> {
            PowerQueryParser.parse("p:contains(One \" One)")
        }
    }
}
