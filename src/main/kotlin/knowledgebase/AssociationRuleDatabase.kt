package main.kotlin.knowledgebase

import main.kotlin.commons.EvidenceInterval
import main.kotlin.commons.RuleDatabase
import net.sf.tweety.logics.fol.syntax.FOLAtom
import net.sf.tweety.logics.fol.syntax.FolFormula
import java.util.*

class AssociationRuleDatabase(rulesToEvidence : Map<FolFormula, EvidenceInterval>) : RuleDatabase<FolFormula>(rulesToEvidence)  {
	val assumptionsMade : MutableMap<Set<AssociationInferenceRule>, MutableSet<AssociationInferenceRule>> = mutableMapOf()
	val itemsetSupports : MutableMap<Pair<Set<AssociationInferenceRule>, TreeSet<FOLAtom>>, EvidenceInterval> = mutableMapOf()
	fun addSupport(assumptions : Set<AssociationInferenceRule>, itemset : TreeSet<FOLAtom>, interval : EvidenceInterval) {
		itemsetSupports.put(Pair(assumptions, itemset), interval)
	}
	fun addRule(assumptions : Set<AssociationInferenceRule>, rule : AssociationInferenceRule){
		if(assumptionsMade.containsKey(assumptions)){
			assumptionsMade.get(assumptions)!!.add(rule)
		}
		else {
			assumptionsMade.put(assumptions, mutableSetOf(rule))
		}
	}

}