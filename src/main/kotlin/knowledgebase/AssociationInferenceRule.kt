package main.kotlin.knowledgebase

import main.kotlin.commons.EvidenceInterval
import main.kotlin.commons.InferenceRule
import net.sf.tweety.logics.fol.syntax.Conjunction
import net.sf.tweety.logics.fol.syntax.Disjunction
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.Implication

class AssociationInferenceRule(val antecedent : Conjunction,
							   val consequent : Disjunction,
							   val support : EvidenceInterval,
							   val confidence : EvidenceInterval) : //Resulting Formula : ante => conse
		InferenceRule<FolFormula>(Implication(antecedent, consequent), support){
	constructor(antecedent: FolFormula, consequent: FolFormula, support : EvidenceInterval, confidence: EvidenceInterval):
			this(Conjunction(setOf(antecedent)), Disjunction(setOf(consequent)), support, confidence)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		if (!super.equals(other)) return false

		other as AssociationInferenceRule

		if (antecedent != other.antecedent) return false
		if (consequent != other.consequent) return false
		if (support != other.support) return false
		if (confidence != other.confidence) return false

		return true
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + antecedent.hashCode()
		result = 31 * result + consequent.hashCode()
		result = 31 * result + support.hashCode()
		result = 31 * result + confidence.hashCode()
		return result
	}

	override fun toString(): String {
		return "AIR: $antecedent=>$consequent, support=$support, confidence=$confidence"
	}
}