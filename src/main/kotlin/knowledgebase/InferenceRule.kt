package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula

open class InferenceRule(val formula : Formula, val evidence : EvidenceInterval){
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as InferenceRule

		if (formula != other.formula) return false
		if (evidence != other.evidence) return false

		return true
	}

	override fun hashCode(): Int {
		var result = formula.hashCode()
		result = 31 * result + evidence.hashCode()
		return result
	}

	override fun toString(): String {
		return "InferenceRule(formula=$formula, evidence=$evidence)"
	}

}