package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula

class InferenceRuleLearner(val config : InferenceRuleLearnerConfig, val rules : Set<InferenceRule>){
	fun findRules(instances : Set<Instance>) : Set<InferenceRule>{
		//First, we need to generate the rules we want to check.
		val possibleRules = mutableSetOf<Formula>()
		if(config.target == null){
			//TODO: Generate ALL POSSIBLE RULES
		}
		else {
			//Only generate rules that are of the form:
			//Formula -> target
		}
		val generatedRules = mutableSetOf<InferenceRule>()
		for(rule in possibleRules){
			val ruleIntervals = testRule(rule, instances) //Already filtered.
			for(interval in ruleIntervals){
				generatedRules.add(InferenceRule(rule, interval))
			}
		}
		return generatedRules.toSet()
	}
	fun testRule(ruleFormula : Formula, instances : Set<Instance>) : Set<ConfidenceInterval> {
		val toReturn = mutableSetOf<ConfidenceInterval>()
		for(instance in instances){
			val intervals = instance.infer(ruleFormula, rules, config.inferenceDepth)
			intervals.forEach { rulesUsed, interval ->
				if(config.filter(interval)){
					toReturn.add(interval)
				}
			}
		}
		return toReturn.toSet()
	}
}