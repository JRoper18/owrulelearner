package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula

open class InferenceRuleLearnerConfig(val inferenceDepth : Int = 1, val target : Formula? = null, val filter : (InferenceRule) -> Boolean = {
	true
}, val sorting : Comparator<ConfidenceInterval>? = null, val maxIntervals : Int = -1) {
}