package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import net.sf.tweety.logics.fol.syntax.Conjunction
import net.sf.tweety.logics.fol.syntax.Negation
import java.lang.IllegalArgumentException

class AssociationInferenceRuleLearner(config : InferenceRuleLearnerConfig, rules : Set<InferenceRule> = setOf()) : InferenceRuleLearner(config, rules){
	override fun testRule(rule: InferenceRule, instances: Set<Instance>) : Map<Set<InferenceRule>, InferenceRule>{
		if(rule is AssociationInferenceRule){
			 //Support = count(antecedent ^ consequent)
			val positiveExamples = countTotal(Conjunction(rule.antecedent, rule.consequent), instances)
			val antecedentCounts = countTotal(rule.antecedent, instances)
			val negativeExamples = countTotal(Conjunction(rule.antecedent, Negation(rule.consequent)), instances)
			val results = mutableMapOf<Set<InferenceRule>, InferenceRule>()
			val possibleRulesUsed = positiveExamples.keys.intersect(negativeExamples.keys).intersect(antecedentCounts.keys)
			possibleRulesUsed.forEach { rulesUsed ->
				val anteCount = antecedentCounts.get(rulesUsed)!!.evidence.positiveInterval().upperBound
				val posInterval = positiveExamples.get(rulesUsed)!!.evidence.positiveInterval()
				val negInterval = negativeExamples.get(rulesUsed)!!.evidence.positiveInterval()
				val createdRule = AssociationInferenceRule(rule.antecedent, rule.consequent,
						positiveExamples.get(rulesUsed)!!.evidence,
						EvidenceInterval(posInterval.lowerBound, negInterval.lowerBound, anteCount))
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