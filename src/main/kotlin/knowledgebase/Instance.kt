package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula

interface Instance {
	fun objects() : Set<String>
	fun query(query : Formula) : TruthValue
	/**
	 * Gives a set of all possible evidence intervals given:
	 * @param query A query to find the evidence interval for
	 * @param rules A set of all known inference rules
	 * @param inferenceDepth The maximum number of inference rules to apply if our answer is initially unknown.
	 * @return A mapping from which inference rules used to the resulting evidence interval.
	 */
	fun infer(query : Formula, rules : Set<InferenceRule>, inferenceDepth : Int) :  Map<Set<InferenceRule>, EvidenceInterval>

	/**
	 * Returns the number of times a query is satisfied, dissatisfied, and left unknown in an instance.
	 * The formula checks all unbound variables against all possible combinations on constants.
	 */
	fun count(query : Formula) : EvidenceInterval
}