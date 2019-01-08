package test.kotlin.knowledgebase

import main.kotlin.knowledgebase.EvidenceInterval
import main.kotlin.knowledgebase.InferenceRule
import main.kotlin.knowledgebase.TruthValue
import main.kotlin.knowledgebase.TweetyFolInstance
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.syntax.FolBeliefSet
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.FolSignature
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TweetyFolInstanceTest {
	val sig = FolSignature(true)
	val parser = FolParser()
	val i1 : TweetyFolInstance
	val i2 : TweetyFolInstance
	val i3 : TweetyFolInstance
	val i4 : TweetyFolInstance
	init {
		sig.add(Constant("apple"))
		sig.add(Constant("banana"))
		sig.add(Predicate("isRed", 1))
		sig.add(Predicate("isApple", 1))
		sig.add(Predicate("isGala", 1))
		parser.signature = sig
		val f1 = parser.parseFormula("isRed(apple)") as FolFormula
		val f2 = parser.parseFormula("!isRed(banana)") as FolFormula
		val f3 = parser.parseFormula("isApple(apple)") as FolFormula
		val f4 = parser.parseFormula("!isApple(banana)") as FolFormula
		i1 = TweetyFolInstance(parser, FolBeliefSet(setOf(f1, f2, f3, f4)))
		i2 = TweetyFolInstance(parser, FolBeliefSet(setOf(f1, f3, f4)))
		i3 = TweetyFolInstance(parser, FolBeliefSet(setOf(f3, f4)))
		i4 = TweetyFolInstance(parser, FolBeliefSet(setOf(f2, f3, f4)))
	}

	@Test
	fun testQuery() {
		println(i1.query(parser.parseFormula("isApple(apple)")))
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
		assertEquals(EvidenceInterval(1, 1, 2), i1.count("(isApple(X) && isRed(X))"))
		assertEquals(EvidenceInterval(1, 1, 2), i2.count("(isApple(X) && isRed(X))"))
		assertEquals(EvidenceInterval(1, 0, 2), i2.count("(isApple(X) || isRed(X))"))

	}

	@Test
	fun testInfer(){
		assertEquals(mapOf(setOf<InferenceRule>() to EvidenceInterval(1, 0, 1)), i1.infer("forall X: (isApple(X) => isRed(X))", setOf(), 1))
		assertEquals(mapOf(setOf<InferenceRule>() to EvidenceInterval(2, 0, 2)), i1.infer("(isApple(X) => isRed(X))", setOf(), 1))

		//Now test it with given inference rules.
		//Imagine this rule is given 5 examples : 4 gala apples, and one granny smith.
		val r1 = InferenceRule(parser.parseFormula("isApple(X) => isRed(X)"), EvidenceInterval(4, 1, 5))
		//This rule is given the 4 gala apples.
		val r2 = InferenceRule(parser.parseFormula("isGala(X) => isRed(X)"), EvidenceInterval(4, 0, 4))
		//This rule could also be mined:
		val r3 = InferenceRule(parser.parseFormula("isApple(X) => isGala(X)"), EvidenceInterval.POSITIVE.scale(4.0))
		val galaInfo = parser.parseFormula("isGala(apple)") as FolFormula
		val appleInfo = parser.parseFormula("isApple(apple)") as FolFormula
		val i5 = TweetyFolInstance(parser, FolBeliefSet(setOf(appleInfo, galaInfo)))
		//No inference depth = no inference.
		assertEquals(mapOf(setOf<InferenceRule>() to EvidenceInterval(0, 0, 1)), i5.infer(parser.parseFormula("isRed(apple)"), setOf(r1, r2), 0))
		//Now, try only using a single inference rule.
		assertEquals(mapOf(setOf<InferenceRule>() to EvidenceInterval(0, 0, 1),
				setOf(r2) to EvidenceInterval(1, 0, 1),
				setOf(r1) to EvidenceInterval(0.8, 0.2, 1.0)
		), i5.infer(parser.parseFormula("isRed(apple)"), setOf(r1, r2), 1))
		//Using both inference rules should be the same, as either of them on their own produces a result.
		assertEquals(mapOf(setOf<InferenceRule>() to EvidenceInterval(0, 0, 1),
				setOf(r2) to EvidenceInterval(1, 0, 1),
				setOf(r1) to EvidenceInterval(0.8, 0.2, 1.0)
		), i5.infer(parser.parseFormula("isRed(apple)"), setOf(r1, r2), 2))
		val i6 = TweetyFolInstance(parser, FolBeliefSet(setOf(galaInfo)))
		//Try using the extra "all galas are apples" rule if we know we have a gala, but not that galas are red.
		assertEquals(mapOf(setOf<InferenceRule>() to EvidenceInterval(0, 0, 1),
				setOf(r2) to EvidenceInterval(1, 0, 1),
				setOf(r3) to EvidenceInterval(0, 0, 1),
				setOf(r2, r3) to EvidenceInterval.POSITIVE
		), i6.infer(parser.parseFormula("isRed(apple)"), setOf(r2, r3), 2))
	}

	@Test
	fun testObjects() {
		//TODO: Write tests for this
	}
}