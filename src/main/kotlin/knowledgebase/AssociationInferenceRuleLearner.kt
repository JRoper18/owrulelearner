package main.kotlin.knowledgebase

import main.kotlin.commons.*
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.commons.syntax.Variable
import net.sf.tweety.logics.fol.syntax.*
import java.lang.IllegalArgumentException
import java.util.*

class AssociationInferenceRuleLearner(config : AssociationInferenceRuleLearnerConfig, rules : Set<AssociationInferenceRule> = setOf()) : InferenceRuleLearner<FolFormula>(config, rules){
	override fun testRule(rule: FolFormula, instances: Set<Instance<FolFormula>>) : Map<Set<InferenceRule<FolFormula>>, AssociationInferenceRule> {
		if (rule is Implication) {
			val antecedent = rule.formulas.first as FolFormula
			val consequent = rule.formulas.second as FolFormula
			//Support = count(antecedent ^ consequent)
			val positiveExamples = countTotal(Conjunction(antecedent, consequent), instances)
			val antecedentCounts = countTotal(antecedent, instances)
			val negativeExamples = countTotal(Conjunction(antecedent, Negation(consequent)), instances)
			val results = mutableMapOf<Set<InferenceRule<FolFormula>>, AssociationInferenceRule>()
			val possibleRulesUsed = positiveExamples.keys.intersect(negativeExamples.keys).intersect(antecedentCounts.keys)
			possibleRulesUsed.forEach { rulesUsed ->
				val anteCount = antecedentCounts.get(rulesUsed)!!.positiveInterval().upperBound
				val posInterval = positiveExamples.get(rulesUsed)!!.positiveInterval()
				val negInterval = negativeExamples.get(rulesUsed)!!.positiveInterval()
				val createdRule = AssociationInferenceRule(antecedent, consequent,
						positiveExamples.get(rulesUsed)!!,
						EvidenceInterval(posInterval.lowerBound, negInterval.lowerBound, anteCount))
				if (config.filter(createdRule)) {
					results.put(rulesUsed, createdRule)
				}
			}
			return results
		}
		else {
			throw IllegalArgumentException("This rule learner only deals with association inference rules of the form (X => Y). ")
		}
	}
	override fun findRules(instances: Set<Instance<FolFormula>>): Map<Set<InferenceRule<FolFormula>>, RuleDatabase<FolFormula>> {
		if(config.target != null && config.target is FolFormula){
			val dontAppendToAntecedent = mutableSetOf<FOLAtom>()
			dontAppendToAntecedent.addAll(config.target.atoms.toSet() as Set<FOLAtom>)
			//Assume all instances share at least the functions and predicates in their signature.
			//Also, we want to try all "forall" rules.
			val sig = (instances.first() as TweetyFolInstance).parser.signature
			val vars = config.target.unboundVariables
			//We're doing apriori, general-to-specific, which means starting with the simplest rule possible.
			val generatedRules : MutableMap<Set<InferenceRule<FolFormula>>, MutableSet<InferenceRule<FolFormula>>> = mutableMapOf()
			testRule(Implication(Tautology(), config.target), instances).forEach { rulesUsed, genedNullRule ->
				generatedRules.put(rulesUsed, mutableSetOf(genedNullRule))
			}
			//Now do single rules
			val frequentItemsets = mutableSetOf(sortedSetOf<FOLAtom>(Comparator { o1, o2 ->
				o1.toString().compareTo(o2.toString())
			}))
			val frequentItemsetsForEvidence : Map<Set<InferenceRule<FolFormula>>, MutableSet<TreeSet<FOLAtom>>> = mutableMapOf()
			val firstLvlFormulas = (generateSupersetFormulas(Conjunction(), dontAppendToAntecedent, sig.predicates, vars))
			for(firstLvlFormula in firstLvlFormulas){
				val firstLvlRules = testRule(Implication(firstLvlFormula, config.target), instances)
				firstLvlRules.forEach { rulesUsed, resultingRule ->
					if(generatedRules.containsKey(rulesUsed)){
						generatedRules.get(rulesUsed)!!.add(resultingRule)
					}
					else {
						generatedRules.put(rulesUsed, mutableSetOf(resultingRule))
					}
				}
			}
			//Now, do F_k-1 X F_k-1 merging to find frequent itemsets.

			
		}
		return mapOf()

	}

	fun generateSupersetFormulas(current : Conjunction, infrequent : Set<FOLAtom>, predicates : Set<Predicate>, variables : Set<Variable>) : List<Conjunction>{
		val supersetFormulas = mutableListOf<Conjunction>()
		for(pred in predicates){
			val lists = makeVariableList(variables, pred.arity)
			for(varList in lists){
				val newAtom = FOLAtom(pred, varList)
				if(!infrequent.contains(newAtom)){
					val clone = current.clone()
					clone.formulas.add(newAtom)
					supersetFormulas.add(clone)
				}
			}
		}
		return supersetFormulas.toList()
	}
	fun makeVariableList(variables : Set<Variable>, arity : Int) : List<List<Variable>> {
		if(arity == 0){
			return listOf(listOf())
		}
		val shorterLists = makeVariableList(variables, arity - 1)
		val newLists = mutableListOf<List<Variable>>()
		for(list in shorterLists){
			for(variable in variables){
				newLists.add(list + variable)
			}
		}
		return newLists.toList()
	}

}