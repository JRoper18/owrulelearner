package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula

data class InferenceRule(val formula : Formula, val confidence : ConfidenceInterval){

}