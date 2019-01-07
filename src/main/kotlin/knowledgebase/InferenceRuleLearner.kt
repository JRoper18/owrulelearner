package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import net.sf.tweety.logics.fol.syntax.FolFormula
import java.util.*

abstract class InferenceRuleLearner(val config : InferenceRuleLearnerConfig, val rules : Set<InferenceRule> = setOf()) {
	abstract fun findRules(instances: Set<Instance>): Map<Set<InferenceRule>, Set<InferenceRule>>
	open fun testRule(rule : InferenceRule, instances : Set<Instance>) : Map<Set<InferenceRule>, InferenceRule> {
		val intervals = countTotal(rule.formula, instances)
		val passedRules = mutableMapOf<Set<InferenceRule>, InferenceRule>()
		for(pair in intervals){
			val rulesUsed = pair.key;
			val currentRule = pair.value
			if (config.filter(currentRule)) {
				passedRules.put(rulesUsed, currentRule)
			}
		}
		return passedRules
	}
	fun countTotal(formula : Formula, instances : Set<Instance>) : Map<Set<InferenceRule>, InferenceRule> {
		val toReturn = mutableMapOf<Set<InferenceRule>, InferenceRule>()
		for (instance in instances) {
			val intervals = instance.infer(formula, rules, config.inferenceDepth)
			intervals.forEach { rulesUsed, interval ->
				if(toReturn.containsKey(rulesUsed)){
					toReturn.put(rulesUsed, InferenceRule(formula, toReturn.get(rulesUsed)!!.evidence.add(interval)))
				}
				else{
					toReturn.put(rulesUsed, InferenceRule(formula, interval))
				}
			}
		}
		return toReturn.toMap()
	}
}