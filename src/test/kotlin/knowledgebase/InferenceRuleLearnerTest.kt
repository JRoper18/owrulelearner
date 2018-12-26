package test.kotlin.knowledgebase;

import main.kotlin.knowledgebase.*
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.FolSignature
import org.junit.jupiter.api.Test;
import kotlin.test.assertEquals

internal class InferenceRuleLearnerTest {

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
		val i1 = TweetyFolInstance(parser)
		i1.addFormulas(setOf(f1, f2, f3, f4))
		val i2 = TweetyFolInstance(parser)
		i2.addFormulas(setOf(f1, f3, f4))
		val i3 = TweetyFolInstance(parser)
		i3.addFormulas(setOf(f3, f4))
		val i4 = TweetyFolInstance(parser)
		i4.addFormulas(setOf(f2, f3, f4))
		val config = InferenceRuleLearnerConfig(sorting = Comparator { o1, o2 -> o1.evidence().compareTo(o2.evidence()) })
		val learner = InferenceRuleLearner(config, setOf<InferenceRule>())
		assertEquals(setOf(ConfidenceInterval(2, 0, 4)), learner.testRule(parser.parseFormula("forall X: (isApple(X) => isRed(X))"), setOf(i1, i2, i3, i4)))
		assertEquals(setOf(ConfidenceInterval(1, 0, 3)), learner.testRule(parser.parseFormula("forall X: (isApple(X) => isRed(X))"), setOf(i2, i3, i4)))
		assertEquals(setOf(ConfidenceInterval(2, 0, 2)), learner.testRule(parser.parseFormula("forall X: (isApple(X) => isRed(X))"), setOf(i1, i2)))
	}
}
