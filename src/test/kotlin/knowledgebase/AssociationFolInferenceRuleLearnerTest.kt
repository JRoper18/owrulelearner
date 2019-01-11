package test.kotlin.knowledgebase

import main.kotlin.commons.EvidenceInterval
import main.kotlin.knowledgebase.*
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.syntax.*
import net.sf.tweety.math.Interval
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class AssociationFolInferenceRuleLearnerTest {
	@Test
	fun testCompleteSimpleRuleTesting() {
		val parser = FolParser()
		val learner = AssociationInferenceRuleLearner(AssociationInferenceRuleLearnerConfig(Interval(0.0, 0.0), Interval(0.0, 0.0)))

		parser.signature = parser.parseSignature("Thing = {bread, milk, diapers, beer, eggs, cola}" +
													"\ntype(bought(Thing))")
		parser.signature.addSignature(FolSignature(true))
		val pBread = parser.parseFormula("bought(bread)") as FolFormula
		val pMilk = parser.parseFormula("bought(milk)") as FolFormula
		val pDiapers = parser.parseFormula("bought(diapers)") as FolFormula
		val pBeer = parser.parseFormula("bought(beer)") as FolFormula
		val pEggs = parser.parseFormula("bought(eggs)") as FolFormula
		val pCola = parser.parseFormula("bought(cola)") as FolFormula
		//All instances are "complete", ie. they have no unknown variables.
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

		val resultRule1 = learner.testRule(Implication(pMilk, pBread), setOf(i1, i2, i3, i4, i5)).get(setOf())!!
		assertEquals(EvidenceInterval(3, 1, 4), resultRule1.confidence)
		assertEquals(EvidenceInterval(3, 2, 5), resultRule1.support)

		val resultRule2 = learner.testRule(Implication(pMilk, pDiapers), setOf(i1, i2, i3, i4, i5)).get(setOf())!!
		assertEquals(EvidenceInterval(3, 1, 4), resultRule2.confidence)
		assertEquals(EvidenceInterval(3, 2, 5), resultRule2.support)
	}

	@Test
	fun testPartialSimpleRuleTesting(){
		val parser = FolParser()
		val learner = AssociationInferenceRuleLearner(AssociationInferenceRuleLearnerConfig())

		parser.signature = parser.parseSignature("Thing = {bread, milk, diapers, beer, eggs, cola}" +
				"\ntype(bought(Thing))")
		parser.signature.addSignature(FolSignature(true))
		val pBread = parser.parseFormula("bought(bread)") as FolFormula
		val pMilk = parser.parseFormula("bought(milk)") as FolFormula
		val pDiapers = parser.parseFormula("bought(diapers)") as FolFormula
		val pBeer = parser.parseFormula("bought(beer)") as FolFormula
		val pEggs = parser.parseFormula("bought(eggs)") as FolFormula
		val pCola = parser.parseFormula("bought(cola)") as FolFormula

		val i1 = TweetyFolInstance(parser, FolBeliefSet(setOf((pMilk), Negation(pDiapers), Negation(pBeer), //Unkonwn bread
				Negation(pEggs), Negation(pCola))))
		val i2 = TweetyFolInstance(parser, FolBeliefSet(setOf(pBread, Negation(pMilk), (pDiapers), (pBeer),
				(pEggs), (pCola))))
		val i3 = TweetyFolInstance(parser, FolBeliefSet(setOf(Negation(pBread), pMilk, (pDiapers), (pBeer),
				Negation(pEggs), (pCola))))
		val i4 = TweetyFolInstance(parser, FolBeliefSet(setOf(pBread, (pDiapers), (pBeer), //Unknown milk
				Negation(pEggs), Negation(pCola))))
		val i5 = TweetyFolInstance(parser, FolBeliefSet(setOf(pBread, pMilk, (pDiapers), Negation(pBeer),
				(pEggs), Negation(pCola))))

		val rule1 = AssociationInferenceRule(pBread, pMilk, EvidenceInterval.EMPTY, EvidenceInterval.EMPTY)
		val resultRule1 = learner.testRule(Implication(pBread, pMilk), setOf(i1, i2, i3, i4, i5)).get(setOf())!! as AssociationInferenceRule
		assertEquals(EvidenceInterval(1, 2, 5), resultRule1.support)
		assertEquals(EvidenceInterval(1, 1, 4), resultRule1.confidence)
	}
}