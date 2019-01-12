package main.kotlin.knowledgebase

import main.kotlin.commons.*
import main.kotlin.util.compareTo
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.commons.syntax.Variable
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
		val database = AssociationRuleDatabase()
		//Assume all instances share at least the functions and predicates in their signature.
		//Also, we want to try all "forall" rules.
		val sig = (instances.first() as TweetyFolInstance).parser.signature
		val vars = setOf<Variable>(Variable("X"), Variable("Y"), Variable("Z")) //Anything that has more than 3 variables is too complicated to be important probably.
		//We're doing apriori, general-to-specific, which means starting with the simplest rule possible: null rules and single-item itemset.
		val firstLevelLiterals = generateBaseFormulas(sig.predicates, vars)
		println(firstLevelLiterals)
		var frequentItemsets = mutableMapOf<Set<AssociationInferenceRule>, MutableSet<TreeSet<FOLLiteral>>>()
		val conf = config as AssociationInferenceRuleLearnerConfig
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
		database.forEachItemset{ assumptions, itemset ->
			//Iterate through each item of the itemset and make it the consequent.
			itemset.forEach { consequent ->
				val clone = TreeSet(itemset)
				println(clone)
				println(assumptions)
				println(database)
				val minimumPositives = database.getSupport(assumptions, clone)!!.positive
				clone.remove(consequent)
				val maxPossibleTotal = database.getSupport(assumptions, clone)!!.positiveInterval().upperBound
				clone.add(consequent.not())
				var minimumNegatives = database.getSupport(assumptions, clone)?.positive
				if(minimumNegatives == null) {
					val negativeExamples = countTotal(makeFormulaFromLiterals(clone), instances)
					negativeExamples.forEach { assumptions, evidence ->
						database.addSupport(assumptions as Set<AssociationInferenceRule>, clone, evidence) //Add it for future use.
					}
					if (negativeExamples.containsKey(assumptions)) {
						minimumNegatives = negativeExamples.get(assumptions)!!.positive
						val confidenceInterval = EvidenceInterval(minimumPositives, minimumNegatives, maxPossibleTotal)
						val confidenceProbability = confidenceInterval.probabilityInterval()
						if (confidenceProbability.lowerBound > config.confidenceInterval.lowerBound && confidenceProbability.upperBound > config.confidenceInterval.upperBound) {
							//Passes confidence tests!
							clone.remove(consequent)
							database.addConfidence(assumptions, clone, consequent, confidenceInterval)
						}
					} else {
						//No assumptions lead here, so we have to discard the data. SAD!
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
					if(sub1.equals(sub2) && !atomList2.last().not().equals(atomList1.last())){
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
	fun generateBaseFormulas(predicates : Set<Predicate>, variables : Set<Variable>, disclude : Set<FOLAtom> = setOf()) : List<FOLLiteral>{
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


