package main.kotlin.commons

import net.sf.tweety.commons.Formula

open class RuleDatabase<T : Formula>(val rulesToEvidences: Map<T, EvidenceInterval>){
}