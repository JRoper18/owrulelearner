package main.kotlin.knowledgebase

import main.kotlin.commons.EvidenceInterval
import main.kotlin.commons.RuleDatabase
import net.sf.tweety.logics.fol.syntax.FolFormula
import java.util.*

class AssociationRuleDatabase(sampleSize : Int, assumptionsToRules : MutableMap<Set<AssociationInferenceRule>, MutableSet<AssociationInferenceRule>> = mutableMapOf()) : RuleDatabase<FolFormula, AssociationInferenceRule>(sampleSize, assumptionsToRules)  {
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
		if(itemset.isEmpty()){ //Null set is a tautology.
			return EvidenceInterval.POSITIVE.scale(sampleSize)
		}
		return itemsetSupports.get(Pair(assumptions, itemset))
	}
	fun getConfidence(assumptions: Set<AssociationInferenceRule>, antecedent : TreeSet<FOLLiteral>, consequent : FOLLiteral) : EvidenceInterval? {
		if(antecedent.isEmpty()){ //Null antecedent is tautology => consequent.
			return getSupport(assumptions, sortedSetOf(consequent))
		}
		return ruleConfidences.get(Triple(assumptions, antecedent, consequent))
	}
	fun itemsetIterator() : Iterator<Map.Entry<Pair<Set<AssociationInferenceRule>, TreeSet<FOLLiteral>>, EvidenceInterval>>{
		return itemsetSupports.iterator()
	}
	fun ruleIterator() : Iterator<Map.Entry<Triple<Set<AssociationInferenceRule>, TreeSet<FOLLiteral>, FOLLiteral>, EvidenceInterval>> {
		return ruleConfidences.iterator()
	}

	override fun toString() : String{
		return itemsetSupports.toString()
	}
	fun size() : Int {
		return ruleConfidences.size
	}

}