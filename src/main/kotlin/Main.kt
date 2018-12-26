package main.kotlin

import main.kotlin.knowledgebase.InferenceRule
import main.kotlin.knowledgebase.InferenceRuleLearner
import main.kotlin.knowledgebase.InferenceRuleLearnerConfig
import main.kotlin.knowledgebase.TweetyFolInstance
import net.sf.tweety.commons.BeliefSet
import net.sf.tweety.logics.commons.LogicalSymbols
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.reasoner.FolReasoner
import net.sf.tweety.logics.fol.syntax.FolBeliefSet
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.FolSignature
import net.sf.tweety.logics.pl.syntax.PropositionalFormula
import net.sf.tweety.logics.pl.parser.PlParser
import net.sf.tweety.logics.pl.syntax.PlBeliefSet
import net.sf.tweety.logics.pl.sat.Sat4jSolver
import net.sf.tweety.logics.pl.sat.SatSolver



fun main(args: Array<String>){
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
	val i2 = TweetyFolInstance(parser);
	i2.addFormulas(setOf(f1, f3, f4))
	val i3 = TweetyFolInstance(parser)
	i3.addFormulas(setOf(f3, f4))
	val i4 = TweetyFolInstance(parser)
	i4.addFormulas(setOf(f2, f3, f4))
	val config = InferenceRuleLearnerConfig(sorting = Comparator { o1, o2 -> o1.evidence().compareTo(o2.evidence()) })
	val learner = InferenceRuleLearner(config, setOf<InferenceRule>())
	println("Result: " + learner.testRule(parser.parseFormula("forall X: (isApple(X) => isRed(X))"), setOf(i1, i2, i3, i4)))

}