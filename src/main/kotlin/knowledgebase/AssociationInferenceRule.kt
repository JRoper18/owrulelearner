package main.kotlin.knowledgebase

import net.sf.tweety.logics.fol.syntax.Conjunction
import net.sf.tweety.logics.fol.syntax.Implication

class AssociationInferenceRule(val antecedent : Conjunction,
							   val consequent : Conjunction,
							   val support : EvidenceInterval,
							   val confidence : EvidenceInterval) : //Resulting Formula : pA ^ !nA -> pC ^ !nC
		InferenceRule(Implication(antecedent, consequent), support){
}