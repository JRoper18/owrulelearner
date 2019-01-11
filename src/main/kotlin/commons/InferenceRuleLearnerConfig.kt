package main.kotlin.commons

import net.sf.tweety.commons.Formula

open class InferenceRuleLearnerConfig<T>(val inferenceDepth : Int = 1, val target : Formula? = null, val filter : (InferenceRule<T>) -> Boolean = {
	true
}, val sorting : Comparator<EvidenceInterval>? = null, val maxIntervals : Int = -1) {
}