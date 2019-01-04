package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import java.util.*

class GenericInferenceRuleLearner(config : InferenceRuleLearnerConfig, rules : Set<InferenceRule> = setOf()) : InferenceRuleLearner(config, rules) {

	override fun findRules(instances : Set<Instance>) : Set<InferenceRule>{
		//Assume all instances have the same signature.
		//First, we need to generate the rules we want to check.
		val possibleRules = mutableSetOf<InferenceRule>()
		if(config.target == null){
			//TODO: Generate ALL POSSIBLE RULES AND DESTROY MY COMPUTER
		}
		else {
			//Only generate horn clauses/rules that are of the form:
			//Formula -> target
		}
		val generatedRules = mutableSetOf<InferenceRule>()
		for(rule in possibleRules){
			val ruleIntervals = testRule(rule, instances) //Already filtered.
			for(interval in ruleIntervals){
				generatedRules.add(InferenceRule(rule.formula, interval))
			}
		}
		return generatedRules.toSet()
	}
	override fun testRule(rule : InferenceRule, instances : Set<Instance>) : Set<ConfidenceInterval> {
		var toReturn = mutableSetOf<ConfidenceInterval>(ConfidenceInterval(0.0, 0.0, 0.0))
		for (instance in instances) {
			val newIntervals = mutableSetOf<ConfidenceInterval>()
			val intervals = instance.infer(rule.formula, rules, config.inferenceDepth)
			intervals.forEach { rulesUsed, interval ->
				if (config.filter(InferenceRule(rule.formula, interval))) {
					for (previousInter in toReturn) {
						newIntervals.add(interval.add(previousInter))
					}
				}
			}
			toReturn = newIntervals
		}
		if (config.sorting == null) {
			return toReturn.toSet()
		}
		val sorted = TreeSet<ConfidenceInterval>(config.sorting)
		if (config.maxIntervals > 0) { //Less than or equal to 0 means "just save them all"
			toReturn.forEach {
				sorted.add(it)
				if (sorted.size > config.maxIntervals) {
					sorted.remove(sorted.first())
				}
			}
		} else {
			sorted.addAll(toReturn)
		}
		return sorted
	}
}