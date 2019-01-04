package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import java.lang.IllegalArgumentException

class AssociationInferenceRuleLearner(config : InferenceRuleLearnerConfig, rules : Set<InferenceRule> = setOf()) : InferenceRuleLearner(config, rules){
	override fun testRule(rule: InferenceRule, instances: Set<Instance>): Set<ConfidenceInterval> {
		if(rule is AssociationInferenceRule){

		}
		else{
			throw IllegalArgumentException("Rule must be an association rule!")
		}
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

	}

	override fun findRules(instances: Set<Instance>): Set<InferenceRule> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

}