package main.kotlin.knowledgebase

import main.kotlin.commons.*
import main.kotlin.util.compareTo
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.commons.syntax.Variable
import net.sf.tweety.logics.commons.syntax.interfaces.Term
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.syntax.*
import net.sf.tweety.math.probability.Probability
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
		var sampleSize = 0
		val parser = (instances.first() as TweetyFolInstance).parser
		val sig = parser.signature
		for(instance in instances){
			if(instance !is TweetyFolInstance){
				throw IllegalArgumentException("Only works with Tweety FOL instances (for now). ")
			}
			if(!instance.parser.signature.predicates.equals(sig.predicates)){
				throw IllegalArgumentException("Instances must share predicates. ")
			}
			//Get the total number of constants.
			sampleSize += instance.size()
		}
		val database = AssociationRuleDatabase(sampleSize)
		//Assume all instances share at least the functions and predicates in their signature.
		//Also, we only want to try all "forall" rules. Only generics.
		val conf = config as AssociationInferenceRuleLearnerConfig

		//We're doing apriori, general-to-specific, which means starting with the simplest rule possible: null rules and single-item itemset.
		val firstLevelLiterals = generateBaseFormulas(sig)
		var frequentItemsets = mutableMapOf<Set<AssociationInferenceRule>, MutableSet<TreeSet<FOLLiteral>>>()
		for(literal in firstLevelLiterals){
			val formula = makeFormulaFromLiterals(listOf(literal))
			val testedFormula = countTotal(formula, instances)
			testedFormula.forEach { rulesUsed, support ->
				val supInterval = support.probabilityInterval()
				if (supInterval.lowerBound > config.supportInterval.lowerBound && supInterval.upperBound > config.supportInterval.upperBound) {
					//Good rule, it passes the support thresholds.
					val literalSet = sortedSetOf(literal)
					database.addSupport(rulesUsed as Set<AssociationInferenceRule>, literalSet, support)
					if(!frequentItemsets.containsKey(rulesUsed)){
						frequentItemsets.put(rulesUsed, mutableSetOf(literalSet))
					}
					else{
						frequentItemsets.get(rulesUsed)!!.add(literalSet)
					}
				}
			}
		}
		while(frequentItemsets.isNotEmpty()){ //Generate next-level itemsets until there are none left to generate.
			val nextLevelItemsets = mutableMapOf<Set<AssociationInferenceRule>, MutableSet<TreeSet<FOLLiteral>>>()
			frequentItemsets.forEach { rulesUsed, literals ->
				val nextLevelForAssumptions = makeNextLevelItemsets(literals, database, instances)
				if(nextLevelForAssumptions.isNotEmpty()){ //If there are frequent itemsets, add them to the next iteration.
					nextLevelItemsets.put(rulesUsed, nextLevelForAssumptions)
				}
			}
			frequentItemsets = nextLevelItemsets //Replace our frequent itemsets with the frequent itemsets of next level.
		}

		/*By now we've generated all frequent itemsets for each set of possible assumptions. Probably took years to finish if we're lucky.
		Now, we must calculate confidences and filter them. The equation for confidence for uncertain datasets like this is another interval.
		For a rule X => Y, P = minCount(X^Y), N=minCount(X^!Y), T=maxCount(X)
		We know X^Y because it was frequent, and X for the same reason. We may know X^!Y if it's frequent, but maybe not.
		*/
		val itemsetIterator = database.itemsetIterator()
		itemsetIterator.forEach{ entry->
			val assumptions = entry.key.first
			val itemset = entry.key.second
			println("ITEMSET: " + itemset)
			//Iterate through each item of the itemset and make it the consequent.
			itemset.forEach { consequent ->
				val clone = TreeSet(itemset)
				val support = database.getSupport(assumptions, clone)!!
				val minimumPositives = support.positive
				clone.remove(consequent)
				val maxPossibleTotal = database.getSupport(assumptions, clone)!!.positiveInterval().upperBound
				clone.add(consequent.not())
				var minimumNegatives = database.getSupport(assumptions, clone)?.positive
				if (minimumNegatives == null) {
					val negativeExamples = countTotal(makeFormulaFromLiterals(clone), instances)
					if (negativeExamples.containsKey(assumptions)) {
						minimumNegatives = negativeExamples.get(assumptions)!!.positive
					}
				}
				if(minimumNegatives != null){ //If we could calculate a value, use that value.
					val confidenceInterval = EvidenceInterval(minimumPositives, minimumNegatives, maxPossibleTotal)
					val confidenceProbability = confidenceInterval.probabilityInterval()
					if (confidenceProbability.lowerBound > config.confidenceInterval.lowerBound && confidenceProbability.upperBound > config.confidenceInterval.upperBound) {
						//Passes confidence tests!
						clone.remove(consequent.not())
						database.addConfidence(assumptions, clone, consequent, confidenceInterval)
						database.addRule(assumptions, AssociationInferenceRule(makeFormulaFromLiterals(clone), consequent.toFormula(), support, confidenceInterval))
					}
				}
			}
		}
		return database
	}
	fun makeNextLevelItemsets(previousLevel : Set<TreeSet<FOLLiteral>>, database : AssociationRuleDatabase, instances : Set<Instance<FolFormula>>) : MutableSet<TreeSet<FOLLiteral>>{
		val conf = config as AssociationInferenceRuleLearnerConfig //Smart cast.
		//Now, do F_k-1 X F_k-1 merging to find frequent itemsets.
		val frequent = mutableSetOf<TreeSet<FOLLiteral>>()
		previousLevel.forEach { atomList1 ->
			previousLevel.forEach { atomList2 ->
				val size = atomList1.size
				if(atomList1 != atomList2){
					val sub1 = atomList1.headSet(atomList1.last())
					val sub2 = atomList2.headSet(atomList2.last())
					if(sub1.equals(sub2) && !atomList2.last().atom.equals(atomList1.last().atom)){
						//Because the sets are sorted, if the first size-1 elements of both frequent itemsets are equal, their combination is frequent!
						//Also, we don't want X^!X in a literal set. It's redundant.
						val newItemset = TreeSet<FOLLiteral>(atomList1)
						newItemset.add(atomList2.last())
						val itemsetFreq = countTotal(makeFormulaFromLiterals(newItemset), instances)
						itemsetFreq.forEach { assumptions, support ->
							val realSup = support.probabilityInterval()
							if(realSup.lowerBound > config.supportInterval.lowerBound && realSup.upperBound > config.supportInterval.upperBound){
								//Passes support test!
								database.addSupport(assumptions as Set<AssociationInferenceRule>, newItemset, support)
								frequent.add(newItemset)
							}
						}
					}
				}
			}
		}
		return frequent
	}
	fun generateBaseFormulas(sig : FolSignature, disclude : Set<FOLAtom> = setOf()) : List<FOLLiteral>{
		val supersetFormulas = mutableListOf<FOLLiteral>()
		for(pred in sig.predicates){
			val lists = makeTermList(sig.constants as Set<Term<Any>>, pred.arity)
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
	fun makeTermList(terms : Collection<Term<Any>>, arity : Int) : List<List<Term<Any>>> {
		if(arity == 0){
			return listOf(listOf())
		}
		val shorterLists = makeTermList(terms, arity - 1)
		val newLists = mutableListOf<List<Term<Any>>>()
		for(list in shorterLists){
			for(term in terms){
				newLists.add(list + term)
			}
		}

		return newLists.toList()
	}

	fun makeFormulaFromLiterals(literals : Collection<FOLLiteral>): FolFormula {
		if(literals.isEmpty()){
			return Tautology()
		}
		else if(literals.size == 1){
			val literal = literals.first()
			return literal.toFormula()
		}
		val formula = Conjunction()
		literals.forEach {
			formula.add(it.toFormula()) //The reason I have to directly parse formulas is that variables are cheked for reference equality during reasoning.
		}
		return formula
	}

}


