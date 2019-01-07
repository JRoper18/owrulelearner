package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import net.sf.tweety.logics.fol.syntax.Conjunction
import java.lang.IllegalArgumentException

class AssociationInferenceRuleLearner(config : InferenceRuleLearnerConfig, rules : Set<InferenceRule> = setOf()) : InferenceRuleLearner(config, rules){
	override fun testRule(rule: InferenceRule, instances: Set<Instance>) : Map<Set<InferenceRule>, InferenceRule>{
		if(rule is AssociationInferenceRule){
			 //Support = count(antecedent ^ consequent)
			val supports = countTotal(Conjunction(setOf(rule.antecedent, rule.consequent)), instances)
			val antecedentCounts = countTotal(rule.antecedent, instances)
			val confidences = mutableMapOf<Set<InferenceRule>, EvidenceInterval>()
			val results = mutableMapOf<Set<InferenceRule>, InferenceRule>()
			supports.forEach { rulesUsed, supportRule ->
				val anteCount = antecedentCounts.get(rulesUsed)!!.evidence.positive
				val posConf = supportRule.evidence.positive / anteCount
				val negConf = supportRule.evidence.negative / anteCount
				val createdRule = AssociationInferenceRule(rule.antecedent, rule.consequent, supportRule.evidence, EvidenceInterval(posConf, negConf, anteCount))
				if(config.filter(createdRule)){
					//We have enough positive examples to pass the filter, and less than the maximum negative examples, and it passes filter.
					results.put(rulesUsed, createdRule)
				}
			}
			return results
		}
		else {
			throw IllegalArgumentException("This rule learner only deals with association inference rules!")
		}
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun findRules(instances: Set<Instance>): Map<Set<InferenceRule>, Set<InferenceRule>> {

		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

}