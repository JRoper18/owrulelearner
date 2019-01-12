package main.kotlin.commons

import net.sf.tweety.commons.Formula
import net.sf.tweety.logics.fol.syntax.FolFormula

open class RuleDatabase<U: Formula, T : InferenceRule<U>>(val assumptionsToRules: MutableMap<Set<T>, MutableSet<T>>){
}