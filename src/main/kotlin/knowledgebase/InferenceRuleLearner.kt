package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import java.util.*

class InferenceRuleLearner(val config : InferenceRuleLearnerConfig, val rules : Set<InferenceRule>){
	fun findRules(instances : Set<Instance>) : Set<InferenceRule>{
		//First, we need to generate the rules we want to check.
		val possibleRules = mutableSetOf<Formula>()
		if(config.target == null){
			//TODO: Generate ALL POSSIBLE RULES AND DESTROY MY COMPUTER
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
		var toReturn = mutableSetOf<ConfidenceInterval>(ConfidenceInterval(0.0, 0.0, 0.0))
		for(instance in instances){
			val newIntervals = mutableSetOf<ConfidenceInterval>()
			val intervals = instance.infer(ruleFormula, rules, config.inferenceDepth)
			intervals.forEach { rulesUsed, interval ->
				if(config.filter(interval)){
					for(previousInter in toReturn){
						newIntervals.add(interval.add(previousInter))
					}
				}
			}
			toReturn = newIntervals
		}
		if(config.sorting == null){
			return toReturn.toSet()
		}
		val sorted = TreeSet<ConfidenceInterval>(config.sorting)
		if(config.maxItems > 0){
			toReturn.forEach {
				sorted.add(it)
				if(sorted.size > config.maxItems){
					sorted.remove(sorted.first())
				}
			}
		}
		else {
			sorted.addAll(toReturn)
		}
		return sorted
	}
}