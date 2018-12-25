package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula

data class InferenceRuleLearnerConfig(val inferenceDepth : Int = 1, val target : Formula? = null, val filter : (ConfidenceInterval) -> Boolean = {
	true
}) {
}