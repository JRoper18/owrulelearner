package test.kotlin.knowledgebase;

import main.kotlin.knowledgebase.*
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.syntax.FolBeliefSet
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.FolSignature
import org.junit.jupiter.api.Test;
import kotlin.test.assertEquals

internal class GenericInferenceRuleLearnerTest {

	@Test
	fun testSingleRule(){
		val sig = FolSignature(true)
		sig.add(Constant("apple"))
		sig.add(Constant("banana"))
		sig.add(Predicate("isRed", 1))
		sig.add(Predicate("isApple", 1))
		val parser = FolParser()
		parser.signature = sig
		val f1 = parser.parseFormula("isRed(apple)") as FolFormula
		val f2 = parser.parseFormula("!isRed(banana)") as FolFormula
		val f3 = parser.parseFormula("isApple(apple)") as FolFormula
		val f4 = parser.parseFormula("!isApple(banana)") as FolFormula
		val i1 = TweetyFolInstance(parser, FolBeliefSet(setOf(f1, f2, f3, f4)))
		val i2 = TweetyFolInstance(parser, FolBeliefSet(setOf(f1, f3, f4)))
		val i3 = TweetyFolInstance(parser, FolBeliefSet(setOf(f3, f4)))
		val i4 = TweetyFolInstance(parser, FolBeliefSet(setOf(f2, f3, f4)))
		val i5 = TweetyFolInstance(parser, FolBeliefSet(setOf(f1, f2)))
		val i6 = TweetyFolInstance(parser, FolBeliefSet(setOf(f1)))
		val config = InferenceRuleLearnerConfig(sorting = Comparator { o1, o2 -> o1.evidence().compareTo(o2.evidence()) })
		val learner = GenericInferenceRuleLearner(config, setOf<InferenceRule>())
		assertEquals(setOf(ConfidenceInterval(6, 0, 8)), learner.testRule(parser.parseFormula("(isApple(X) => isRed(X))"), setOf(i1, i2, i3, i4)))
		/*
		assertEquals(setOf(ConfidenceInterval(1, 0, 3)), learner.testRule(parser.parseFormula("(isApple(X) => isRed(X))"), setOf(i2, i3, i4)))
		assertEquals(setOf(ConfidenceInterval(2, 0, 2)), learner.testRule(parser.parseFormula("(isApple(X) => isRed(X))"), setOf(i1, i2)))
		assertEquals(setOf(ConfidenceInterval(0, 0, 2)), learner.testRule(parser.parseFormula("(isApple(X) => isRed(X))"), setOf(i5)))
		assertEquals(setOf(ConfidenceInterval(0, 0, 3)), learner.testRule(parser.parseFormula("(isApple(X) => isRed(X))"), setOf(i5, i6)))
		*/
	}
}