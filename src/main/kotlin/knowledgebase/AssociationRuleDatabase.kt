package main.kotlin.knowledgebase

import main.kotlin.commons.EvidenceInterval
import main.kotlin.commons.RuleDatabase
import net.sf.tweety.logics.fol.syntax.FolFormula
import java.util.*

class AssociationRuleDatabase(assumptionsToRules : MutableMap<Set<AssociationInferenceRule>, MutableSet<AssociationInferenceRule>> = mutableMapOf()) : RuleDatabase<FolFormula, AssociationInferenceRule>(assumptionsToRules)  {
	private val itemsetSupports : MutableMap<Pair<Set<AssociationInferenceRule>, TreeSet<FOLLiteral>>, EvidenceInterval> = mutableMapOf()
	private val ruleConfidences : MutableMap<Triple<Set<AssociationInferenceRule>, TreeSet<FOLLiteral>, FOLLiteral>, EvidenceInterval> = mutableMapOf()
	fun addSupport(assumptions : Set<AssociationInferenceRule>, itemset : TreeSet<FOLLiteral>, interval : EvidenceInterval) {
		itemsetSupports.put(Pair(assumptions, itemset), interval)
	}
	fun addConfidence(assumptions: Set<AssociationInferenceRule>, antecedent : TreeSet<FOLLiteral>, consequent : FOLLiteral, interval : EvidenceInterval){
		ruleConfidences.put(Triple(assumptions, antecedent, consequent), interval)
	}
	fun addRule(assumptions : Set<AssociationInferenceRule>, rule : AssociationInferenceRule){
		if(assumptionsToRules.containsKey(assumptions)){
			assumptionsToRules.get(assumptions)!!.add(rule)
		}
		else {
			assumptionsToRules.put(assumptions, mutableSetOf(rule))
		}
	}
	fun getSupport(assumptions: Set<AssociationInferenceRule>, itemset : TreeSet<FOLLiteral>) : EvidenceInterval? {
		return itemsetSupports.get(Pair(assumptions, itemset))
	}
	fun getConfidence(assumptions: Set<AssociationInferenceRule>, antecedent : TreeSet<FOLLiteral>, consequent : FOLLiteral) : EvidenceInterval? {
		return ruleConfidences.get(Triple(assumptions, antecedent, consequent))
	}
	fun forEachItemset(forEach : (assumptions: Set<AssociationInferenceRule>, itemset : TreeSet<FOLLiteral>) -> Unit){
		itemsetSupports.forEach { a, u ->
			forEach(a.first, a.second)
		}
	}

	override fun toString() : String{
		return itemsetSupports.toString()
	}


}