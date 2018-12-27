package main.kotlin.knowledgebase

import net.sf.tweety.commons.BeliefBase
import net.sf.tweety.commons.Formula
import net.sf.tweety.commons.Parser

interface Instance {
	fun objects() : Set<String>
	fun query(query : Formula) : TruthValue
	/**
	 * Gives a set of all possible confidence intervals given:
	 * @param query A query to find the confidence interval for
	 * @param rules A set of all known inference rules
	 * @param inferenceDepth The maximum number of inference rules to apply if our answer is initially unknown.
	 * @return A mapping from which inference rules used to the resulting confidence interval.
	 */
	fun infer(query : Formula, rules : Set<InferenceRule>, inferenceDepth : Int) :  Map<Set<InferenceRule>, ConfidenceInterval>

	/**
	 * Returns the number of times a query is satisfied, dissatisfied, and left unknown in an instance.
	 */
	fun count(query : Formula) : ConfidenceInterval
}