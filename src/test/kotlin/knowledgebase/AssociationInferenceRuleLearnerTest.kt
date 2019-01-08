package test.kotlin.knowledgebase

import main.kotlin.knowledgebase.*
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.syntax.FolBeliefSet
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.FolSignature
import net.sf.tweety.logics.fol.syntax.Negation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class AssociationInferenceRuleLearnerTest {
	@Test
	fun testCompleteSimpleRuleTesting() {
		val parser = FolParser()
		val learner = AssociationInferenceRuleLearner(InferenceRuleLearnerConfig())

		parser.signature = parser.parseSignature("Thing = {bread, milk, diapers, beer, eggs, cola}" +
													"\ntype(bought(Thing))")
		parser.signature.addSignature(FolSignature(true))
		val pBread = parser.parseFormula("bought(bread)") as FolFormula
		val pMilk = parser.parseFormula("bought(milk)") as FolFormula
		val pDiapers = parser.parseFormula("bought(diapers)") as FolFormula
		val pBeer = parser.parseFormula("bought(beer)") as FolFormula
		val pEggs = parser.parseFormula("bought(eggs)") as FolFormula
		val pCola = parser.parseFormula("bought(cola)") as FolFormula

		val i1 = TweetyFolInstance(parser, FolBeliefSet(setOf(pBread, (pMilk), Negation(pDiapers), Negation(pBeer),
				Negation(pEggs), Negation(pCola))))
		val i2 = TweetyFolInstance(parser, FolBeliefSet(setOf(pBread, Negation(pMilk), (pDiapers), (pBeer),
				(pEggs), (pCola))))
		val i3 = TweetyFolInstance(parser, FolBeliefSet(setOf(Negation(pBread), pMilk, (pDiapers), (pBeer),
				Negation(pEggs), (pCola))))
		val i4 = TweetyFolInstance(parser, FolBeliefSet(setOf(pBread, pMilk, (pDiapers), (pBeer),
				Negation(pEggs), Negation(pCola))))
		val i5 = TweetyFolInstance(parser, FolBeliefSet(setOf(pBread, pMilk, (pDiapers), Negation(pBeer),
				(pEggs), Negation(pCola))))

		val rule1 = AssociationInferenceRule(pBread, pMilk, EvidenceInterval.EMPTY, EvidenceInterval.EMPTY)
		val resultRule1 = learner.testRule(rule1, setOf(i1, i2, i3, i4, i5)).get(setOf())!! as AssociationInferenceRule
		assertEquals(EvidenceInterval(3, 1, 4), resultRule1.confidence)


	}
}