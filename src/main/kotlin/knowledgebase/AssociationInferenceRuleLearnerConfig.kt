package main.kotlin.knowledgebase

import main.kotlin.commons.InferenceRuleLearnerConfig
import main.kotlin.util.compareTo
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.math.Interval
import net.sf.tweety.math.probability.Probability

class AssociationInferenceRuleLearnerConfig(val maxAntecedentLength : Int = 4, val maxNumberAntecedentVariables : Int = 3, val confidenceInterval : Interval<Probability> = Interval(Probability(0.0), Probability(0.0)), val supportInterval : Interval<Probability> = Interval(Probability(0.0), Probability(0.0)), inferenceDepth : Int = 1) : InferenceRuleLearnerConfig<FolFormula>(inferenceDepth, filter = {
	var res : Boolean = false
	if(it is AssociationInferenceRule){
		val realConf = it.confidence.probabilityInterval()
		if(realConf.lowerBound > confidenceInterval.lowerBound && realConf.upperBound > confidenceInterval.upperBound){
			val realSup = it.support.probabilityInterval()
			if(realSup.lowerBound > supportInterval.lowerBound && realSup.upperBound > supportInterval.upperBound){
				res = true
			}
		}
	}
	res
})