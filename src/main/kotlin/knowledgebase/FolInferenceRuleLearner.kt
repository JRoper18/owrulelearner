package main.kotlin.knowledgebase

import main.kotlin.commons.*
import net.sf.tweety.logics.fol.syntax.FolFormula

class FolInferenceRuleLearner(config : InferenceRuleLearnerConfig<FolFormula>, rules : Set<InferenceRule<FolFormula>> = setOf()) : InferenceRuleLearner<FolFormula>(config, rules) {
	override fun findRules(instances: Set<Instance<FolFormula>>): Map<Set<InferenceRule<FolFormula>>, RuleDatabase<FolFormula>> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}