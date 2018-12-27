package test.kotlin.knowledgebase

import main.kotlin.knowledgebase.ConfidenceInterval
import main.kotlin.knowledgebase.InferenceRule
import main.kotlin.knowledgebase.TruthValue
import main.kotlin.knowledgebase.TweetyFolInstance
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.FolSignature
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TweetyFolInstanceTest {
	val sig = FolSignature(true)
	val parser = FolParser()
	val i1 = TweetyFolInstance(parser)
	val i2 = TweetyFolInstance(parser)
	val i3 = TweetyFolInstance(parser)
	val i4 = TweetyFolInstance(parser)
	init {
		sig.add(Constant("apple"))
		sig.add(Constant("banana"))
		sig.add(Predicate("isRed", 1))
		sig.add(Predicate("isApple", 1))
		parser.signature = sig
		val f1 = parser.parseFormula("isRed(apple)") as FolFormula
		val f2 = parser.parseFormula("!isRed(banana)") as FolFormula
		val f3 = parser.parseFormula("isApple(apple)") as FolFormula
		val f4 = parser.parseFormula("!isApple(banana)") as FolFormula
		i1.addFormulas(setOf(f1, f2, f3, f4))
		i2.addFormulas(setOf(f1, f3, f4))
		i3.addFormulas(setOf(f3, f4))
		i4.addFormulas(setOf(f2, f3, f4))

	}

	@Test
	fun testQuery() {

		assertEquals(TruthValue.TRUE, i1.query(parser.parseFormula("forall X: (isApple(X) => isRed(X))")))
		assertEquals(TruthValue.TRUE, i2.query(parser.parseFormula("forall X: (isApple(X) => isRed(X))")))
		assertEquals(TruthValue.UNKNOWN, i3.query(parser.parseFormula("forall X: (isApple(X) => isRed(X))")))
		assertEquals(TruthValue.UNKNOWN, i4.query(parser.parseFormula("forall X: (isApple(X) => isRed(X))")))

		//Let's try some other logical operators in our queries.
		assertEquals(TruthValue.TRUE, i1.query("isApple(apple) && !isApple(banana)"))
		assertEquals(TruthValue.FALSE, i1.query("isApple(apple) && isApple(banana)"))

		assertEquals(TruthValue.UNKNOWN, i2.query("isRed(apple) && isRed(banana)")) //Don't know the color of banana in i2

		assertEquals(TruthValue.TRUE, i2.query("isRed(apple) || isRed(banana)"))
		assertEquals(TruthValue.UNKNOWN, i3.query("isRed(apple) || isRed(banana)"))

		assertEquals(TruthValue.TRUE, i1.query("exists X: (!isRed(X))"))
		assertEquals(TruthValue.UNKNOWN, i2.query("exists X: (!isRed(X))"))
		assertEquals(TruthValue.UNKNOWN, i3.query("exists X: (!isRed(X))"))

		assertEquals(TruthValue.TRUE, i3.query("isRed(banana) || !isRed(banana)")) //Should still detect logical "obviousness"
	}

	@Test
	fun testCount(){
		assertEquals(ConfidenceInterval(1, 0, 1), i1.count("forall X: (isApple(X) => isRed(X))"))
		assertEquals(ConfidenceInterval(1, 0, 1), i2.count("forall X: (isApple(X) => isRed(X))"))
	}

	@Test
	fun testInfer(){
		assertEquals(mapOf(setOf<InferenceRule>() to ConfidenceInterval(1, 0, 1)), i1.infer("forall X: (isApple(X) => isRed(X))", setOf(), 1))
	}

	@Test
	fun testObjects() {
		//TODO: Write tests for this
	}
}