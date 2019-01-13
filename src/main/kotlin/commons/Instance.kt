package main.kotlin.commons

import net.sf.tweety.commons.BeliefBase
import net.sf.tweety.commons.Formula
import net.sf.tweety.commons.Parser

interface Instance<T : Formula> {
	fun query(query : T) : TruthValue
	/**
	 * Gives a set of all possible evidence intervals given:
	 * @param query A query to find the evidence interval for
	 * @param rules A set of all known inference rules
	 * @param inferenceDepth The maximum number of inference rules to apply if our answer is initially unknown.
	 * @return A mapping from which inference rules used to the resulting evidence interval.
	 */
	fun infer(query : T, rules : Set<InferenceRule<T>>, inferenceDepth : Int) :  Map<Set<InferenceRule<T>>, EvidenceInterval>

	/**
	 * Returns the number of times a query is satisfied, dissatisfied, and left unknown in an instance.
	 * The formula checks all unbound variables against all possible combinations on constants.
	 */
	fun count(query : T) : EvidenceInterval

	/**
	 * The "size" of the instance, or how many times count can check for something.
	 * - In FOL, it's the number of possible constants.
	 * - In propositional logic, it's 1.
	 */
	fun size() : Int
}