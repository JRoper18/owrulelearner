package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula

abstract class InferenceRuleLearner(val config : InferenceRuleLearnerConfig, val rules : Set<InferenceRule> = setOf()) {
	abstract fun findRules(instances: Set<Instance>): Set<InferenceRule>
	abstract fun testRule(rule : InferenceRule, instances : Set<Instance>) : Set<ConfidenceInterval>

}