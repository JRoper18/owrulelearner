package main.kotlin.knowledgebase

import main.kotlin.commons.InferenceRule
import main.kotlin.commons.RuleDatabase
import net.sf.tweety.logics.fol.syntax.FolFormula

abstract class FolRuleDatabase<T : InferenceRule<FolFormula>>(sampleSize : Int, assumptionsToRules : MutableMap<Set<T>, MutableSet<T>>) : RuleDatabase<FolFormula, T>(sampleSize, assumptionsToRules) {
}