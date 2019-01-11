package main.kotlin.commons

import net.sf.tweety.commons.Formula
import net.sf.tweety.logics.fol.syntax.FolFormula

abstract class InferenceRuleLearner<T : Formula>(val config : InferenceRuleLearnerConfig<T>, val rules : Set<InferenceRule<T>> = setOf()) {
	abstract fun findRules(instances: Set<Instance<T>>): Map<Set<InferenceRule<T>>, RuleDatabase<FolFormula>>
	open fun testRule(rule : T, instances : Set<Instance<T>>) : Map<Set<InferenceRule<T>>, InferenceRule<T>> {
		val intervals = countTotal(rule, instances)
		val passedRules = mutableMapOf<Set<InferenceRule<T>>, InferenceRule<T>>()
		for(pair in intervals){
			val rulesUsed = pair.key
			val currentRule : InferenceRule<T> = InferenceRule<T>(rule, pair.value)
			if (config.filter(currentRule)) {
				passedRules.put(rulesUsed, currentRule)
			}
		}
		return passedRules
	}
	fun countTotal(formula : T, instances : Set<Instance<T>>) : Map<Set<InferenceRule<T>>, EvidenceInterval> {
		val toReturn = mutableMapOf<Set<InferenceRule<T>>, EvidenceInterval>()
		for (instance in instances) {
			val intervals = instance.infer(formula, rules, config.inferenceDepth)
			intervals.forEach { rulesUsed, interval ->
				if(toReturn.containsKey(rulesUsed)){
					toReturn.put(rulesUsed, toReturn.get(rulesUsed)!!.add(interval))
				}
				else{
					toReturn.put(rulesUsed, interval)
				}
			}
		}
		return toReturn.toMap()
	}
}