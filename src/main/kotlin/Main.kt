package main.kotlin

import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.syntax.FolSignature
import net.sf.tweety.logics.fol.syntax.FolBeliefSet
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.reasoner.FolReasoner
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.cl.semantics.RankingFunction.satisfies
import net.sf.tweety.logics.pl.syntax.PropositionalSignature
import net.sf.tweety.logics.pl.semantics.PossibleWorld
import net.sf.tweety.logics.pl.syntax.Proposition
import net.sf.tweety.logics.pl.syntax.PlBeliefSet
import net.sf.tweety.logics.pl.syntax.PropositionalFormula
import net.sf.tweety.beliefdynamics.DefaultMultipleBaseExpansionOperator
import net.sf.tweety.arg.deductive.reasoner.SimpleReasoner
import net.sf.tweety.beliefdynamics.kernels.RandomIncisionFunction
import net.sf.tweety.beliefdynamics.kernels.KernelContractionOperator
import net.sf.tweety.beliefdynamics.LeviMultipleBaseRevisionOperator
import net.sf.tweety.beliefdynamics.MultipleBaseRevisionOperator
import net.sf.tweety.logics.pl.sat.SatSolver
import net.sf.tweety.arg.dung.reasoner.SatCompleteReasoner
import net.sf.tweety.arg.dung.syntax.Argument
import net.sf.tweety.arg.dung.syntax.Attack
import net.sf.tweety.arg.dung.syntax.DungTheory
import net.sf.tweety.logics.pl.reasoner.NaiveReasoner


fun main(args: Array<String>){
	val theory = DungTheory()
	val a = Argument("a")
	val b = Argument("b")
	val c = Argument("c")
	theory.add(a)
	theory.add(b)
	theory.add(c)
	theory.add(Attack(a, b))
	theory.add(Attack(b, c))
	theory.add(Attack(c, b))
	theory.add(Attack(c, a))

	val reasoner = SatCompleteReasoner(SatSolver.getDefaultSolver())

	println(reasoner.getModels(theory))
	println()

	val beliefSet = reasoner.getPropositionalCharacterisation(theory)
	println(beliefSet)
	println()
	for (w in PossibleWorld.getAllPossibleWorlds(beliefSet.signature as PropositionalSignature)) {
		if (w.satisfies(beliefSet))
			println(w)
	}

	val revise = LeviMultipleBaseRevisionOperator(
			KernelContractionOperator(RandomIncisionFunction(), NaiveReasoner()),
			DefaultMultipleBaseExpansionOperator<PropositionalFormula>())

	val beliefSet2 = PlBeliefSet(revise.revise(beliefSet, Proposition("in_a")))

	println(beliefSet2)
	println()
	for (w in PossibleWorld.getAllPossibleWorlds(beliefSet2.signature as PropositionalSignature)) {
		if (w.satisfies(beliefSet2))
			println(w)
	}
}