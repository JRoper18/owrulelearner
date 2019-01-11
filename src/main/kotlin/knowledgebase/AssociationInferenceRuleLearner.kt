package main.kotlin.knowledgebase

import main.kotlin.commons.*
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.commons.syntax.Variable
import net.sf.tweety.logics.fol.syntax.*
import java.lang.IllegalArgumentException
import java.util.*

class AssociationInferenceRuleLearner(aConfig : AssociationInferenceRuleLearnerConfig, rules : Set<AssociationInferenceRule> = setOf()) : InferenceRuleLearner<FolFormula>(aConfig, rules){
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
	override fun findRules(instances: Set<Instance<FolFormula>>): AssociationRuleDatabase {
		val database = AssociationRuleDatabase()
		if(config.target != null && config.target is FolFormula){
			val dontAppendToAntecedent = mutableSetOf<FOLAtom>()
			dontAppendToAntecedent.addAll(config.target.atoms.toSet() as Set<FOLAtom>)
			//Assume all instances share at least the functions and predicates in their signature.
			//Also, we want to try all "forall" rules.
			val sig = (instances.first() as TweetyFolInstance).parser.signature
			val vars = config.target.unboundVariables
			//We're doing apriori, general-to-specific, which means starting with the simplest rule possible: Null itemset.
			testRule(Implication(Tautology(), config.target), instances).forEach { rulesUsed, genedNullRule ->
				database.addRule(rulesUsed as Set<AssociationInferenceRule>, genedNullRule)
			}
			//Now do single-item rules
			val firstLevelLiterals = generateBaseFormulas(dontAppendToAntecedent, sig.predicates, vars)


		}
		return database
	}
	fun makeNextLevelItemsets(previousLevel : Collection<TreeSet<FOLLiteral>>, database : AssociationRuleDatabase, instances : Set<TweetyFolInstance>) : LinkedList<TreeSet<FOLLiteral>>{
		//Now, do F_k-1 X F_k-1 merging to find frequent itemsets.
		val frequent = LinkedList<TreeSet<FOLLiteral>>()
		previousLevel.forEach { atomList1 ->
			previousLevel.forEach { atomList2 ->
				val size = atomList1.size
				if(atomList1 != atomList2){
					val sub1 = atomList1.headSet(atomList1.last())
					val sub2 = atomList2.headSet(atomList2.last())
					if(sub1.equals(sub2)){
						//Because the sets are sorted, if the first size-1 elements of both frequent itemsets are equal, their combination is frequent!
						val newItemset = TreeSet<FOLLiteral>(sub1)
						newItemset.add(atomList2.last())
						val itemsetFreq = countTotal(makeFormulaFromLiterals(newItemset), instances)
						val supportInterval = config as AssociationInferenceRuleLearnerConfig //Smart cast.
						itemsetFreq.forEach { assumptions, support ->
							val realSup = support.positiveInterval()
							if(realSup.lowerBound > config.supportInterval.lowerBound && realSup.upperBound > config.supportInterval.upperBound){
								//Passes support test!
								frequent.add(newItemset)
							}
						}
					}
				}
			}
		}
		return frequent
	}
	fun generateBaseFormulas(disclude : Set<FOLAtom>, predicates : Set<Predicate>, variables : Set<Variable>) : List<FOLLiteral>{
		val supersetFormulas = mutableListOf<FOLLiteral>()
		for(pred in predicates){
			val lists = makeVariableList(variables, pred.arity)
			for(varList in lists){
				val newAtom = FOLAtom(pred, varList)
				if(!disclude.contains(newAtom)){
					supersetFormulas.add(FOLLiteral(newAtom, true))
					supersetFormulas.add(FOLLiteral(newAtom, false))
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

	fun makeFormulaFromLiterals(literals : Collection<FOLLiteral>): Conjunction {
		val formula = Conjunction()
		literals.forEach {
			if(it.neg){
				formula.add(Negation(it.atom))
			}
			else{
				formula.add(it.atom)
			}
		}
		return formula
	}

}