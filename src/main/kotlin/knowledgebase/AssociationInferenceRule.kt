package main.kotlin.knowledgebase

import net.sf.tweety.logics.fol.syntax.Conjunction
import net.sf.tweety.logics.fol.syntax.FOLAtom
import net.sf.tweety.logics.fol.syntax.Implication
import net.sf.tweety.logics.fol.syntax.Negation

class AssociationInferenceRule(val positiveAntecedents : Collection<FOLAtom>,
							   val negativeAntecedents : Collection<FOLAtom>,
							   val positiveConsequents :  Collection<FOLAtom>,
							   val negativeConsequents : Collection<FOLAtom>,
							   confidence : ConfidenceInterval?) : //Resulting Formula : pA ^ !nA -> pC ^ !nC
		InferenceRule(Implication(
				Conjunction(positiveAntecedents + Negation(Conjunction(negativeAntecedents))),
				Conjunction(positiveConsequents + Negation(Conjunction(negativeConsequents)))), confidence) {


}