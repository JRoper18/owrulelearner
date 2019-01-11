package main.kotlin.knowledgebase

import main.kotlin.commons.InferenceRuleLearnerConfig
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.math.Interval

class AssociationInferenceRuleLearnerConfig(val confidenceInterval : Interval<Double> = Interval(0.0, 0.0), val supportInterval : Interval<Double> = Interval(0.0, 0.0), inferenceDepth : Int = 1) : InferenceRuleLearnerConfig<FolFormula>(inferenceDepth, filter = {
	var res : Boolean = false
	if(it is AssociationInferenceRule){
		val realConf = it.confidence.positiveInterval()
		if(realConf.lowerBound > confidenceInterval.lowerBound && realConf.upperBound > confidenceInterval.upperBound){
			val realSup = it.support.positiveInterval()
			if(realSup.lowerBound > supportInterval.lowerBound && realSup.upperBound > supportInterval.upperBound){
				res = true
			}
		}
	}
	res
})