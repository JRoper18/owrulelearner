package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import java.util.*

class GenericInferenceRuleLearner(config : InferenceRuleLearnerConfig, rules : Set<InferenceRule> = setOf()) : InferenceRuleLearner(config, rules) {

	override fun findRules(instances : Set<Instance>) : Map<Set<InferenceRule>, Set<InferenceRule>> {
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
		val generatedRules = mutableMapOf<Set<InferenceRule>, Set<InferenceRule>>()
		for(rule in possibleRules){
			val resultingRules = testRule(rule, instances) //Already filtered.
			for(testedRules in resultingRules){
				val assumptions = testedRules.key
				val resultingRule = testedRules.value
				if(generatedRules.containsKey(assumptions)){
					val prev = generatedRules.get(assumptions)!!
					prev.toMutableSet().add(resultingRule)
					generatedRules.put(assumptions, prev.toSet())
				}
				else{
					generatedRules.put(assumptions, setOf(resultingRule))
				}
			}
		}
		return generatedRules.toMap()
	}
}