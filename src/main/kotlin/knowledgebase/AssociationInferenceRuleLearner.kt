package main.kotlin.knowledgebase

import main.kotlin.commons.*
import main.kotlin.util.compareTo
import net.sf.tweety.commons.Formula
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Variable
import net.sf.tweety.logics.commons.syntax.interfaces.Term
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
		var sampleSize = 0
		val parser = (instances.first() as TweetyFolInstance).parser
		val sig = parser.signature
		for(instance in instances){
			if(instance !is TweetyFolInstance){
				throw IllegalArgumentException("Only works with Tweety FOL instances (for now). ")
			}
			if(!instance.parser.signature.predicates.equals(sig.predicates)){
				throw IllegalArgumentException("Signature predicates should be the same!")
 			}
			//Get the total number of constants.
			sampleSize += instance.size()
		}
		val database = AssociationRuleDatabase(sampleSize)
		findConstantRules(instances, database)
		//Now from the constant rules, turn them into variable-bound rules.
		val ruleIterator = database.ruleIterator()

		ruleIterator.forEachRemaining {

		}
		return database
	}
	fun findConstantRules(instances: Set<Instance<FolFormula>>, database : AssociationRuleDatabase) {
		//Assume all instances share at least the functions and predicates in their signature.
		//Also, we only want to try all "forall" rules. Only generics.
		val conf = config as AssociationInferenceRuleLearnerConfig
		val sig = (instances.first() as TweetyFolInstance).parser.signature
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
	}
	private fun makeNextLevelItemsets(previousLevel : Set<TreeSet<FOLLiteral>>, database : AssociationRuleDatabase, instances : Set<Instance<FolFormula>>) : MutableSet<TreeSet<FOLLiteral>>{
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
						//Also, we don't want X^!X or X^X in a literal set. It's redundant.
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
	private fun generateBaseVariableFormulas(sig : FolSignature) : List<FOLLiteral> {
		val literals = mutableListOf<FOLLiteral>()
		for(predicate in sig.predicates){
			for(varList in makeVariableList(predicate.arity)){
				literals.add(FOLLiteral(FOLAtom(predicate, varList), true))
				literals.add(FOLLiteral(FOLAtom(predicate, varList), false))
			}
		}
		return literals
	}
	private fun generateBaseFormulas(sig : FolSignature, disclude : Set<FOLAtom> = setOf()) : List<FOLLiteral>{
		val supersetFormulas = mutableListOf<FOLLiteral>()
		for(pred in sig.predicates){
			val lists = makeConstantList(sig.constants, pred.arity)
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
	private fun makeConstantList(terms : Collection<Constant>, arity : Int) : List<List<Constant>> {
		if(arity == 0){
			return listOf(listOf())
		}
		val shorterLists = makeConstantList(terms, arity - 1)
		val newLists = mutableListOf<List<Constant>>()
		for(list in shorterLists){
			for(term in terms){
				newLists.add(list + term)
			}
		}
		return newLists.toList()
	}
	private fun getPossibleVariables(arity : Int) : List<Variable> {
		if(arity == 0){
			return listOf()
		}
		val prev = getPossibleVariables(arity - 2)
		val newVar : Variable;
		if(arity <= 3){ //First x, y, z
			newVar = Variable(((arity - 1) + 'X'.toInt()).toChar().toString())
		}
		else { //Then w, v, and backwards down the alphabet.
			newVar = Variable(('W'.toInt() - arity - 3).toChar().toString())
		}
		return prev + newVar
	}

	/**
	 * Given a list of atoms, check if all the variables in each atom "relate" to the atoms in the target set of atoms.
	 * For example, the statement f(A) => p(B) doesn't relate, as A has no relation to B.
	 * On the other hand, f(A, B) ^ g(B, C) => f(A, C). Even though B doesn't directly relate to A or C in the target, it relates to A
	 * and C by being in the same predicate as A and C in the antecedent.
	 */
	private fun checkIfAtomsRelate(atoms : List<FOLAtom>, target : Set<FOLAtom>) : Boolean {
		var relatedTerms = target.flatMap {
			it.arguments
		}.toMutableSet()
		val uncheckedAtoms : MutableSet<FOLAtom> = atoms.toMutableSet()
		var foundNewAtoms : Boolean = false
		do {
			val toRemove = mutableListOf<FOLAtom>()
			for(atom in uncheckedAtoms){
				for(arg in atom.arguments){
					if(relatedTerms.contains(arg)){
						relatedTerms.addAll(atom.arguments)
						foundNewAtoms = true
						toRemove.add(atom)
						break
					}
				}
			}
			uncheckedAtoms.removeAll(toRemove)
		} while(foundNewAtoms)
		return uncheckedAtoms.isEmpty()
	}
	private fun turnGroundedFormulaIntoGeneral(groundedLiterals : TreeSet<FOLLiteral>) : TreeSet<FOLLiteral> {
		val newLiterals = sortedSetOf<FOLLiteral>()
		val groundsToVars = mutableMapOf<FOLAtom, Variable>()
		val maxArity = groundedLiterals.size
		val possibleVariables = getPossibleVariables(maxArity)
		var currentUsedVarIndex = -1;
		for(literal in groundedLiterals){
			if(!literal.isGround()){
				newLiterals.add(literal)
			}
			else{
				val ungroundVar = groundsToVars.getOrPut(literal.atom, {possibleVariables[currentUsedVarIndex++]})
				newLiterals.add(FOLLiteral(FOLAtom(literal.atom.predicate, ungroundVar), literal.neg))
			}
		}
		return newLiterals
	}
	private fun makeVariableList(arity : Int) : List<List<Variable>>{
		if(arity == 0){
			return listOf(listOf())
		}
		if(variableLists[arity] != null){
			return variableLists[arity]!!
		}
		val shorterLists = makeVariableList(arity - 1)
		val newLists = mutableListOf<List<Variable>>()
		val newVars = getPossibleVariables(arity)
		for(list in shorterLists){
			for(variable in newVars){
				newLists.add(list + variable)
			}
		}
		variableLists[arity] = newLists.toList()
		return newLists.toList()
	}
	private fun makeFormulaFromLiterals(literals : Collection<FOLLiteral>): FolFormula {
		if(literals.isEmpty()){
			return Tautology()
		}
		else if(literals.size == 1){
			val literal = literals.first()
			return literal.toFormula()
		}
		val formula = Conjunction()
		literals.forEach {
			formula.add(it.toFormula())
		}
		return formula
	}
	companion object {
		val variableLists : MutableList<List<List<Variable>>?> = mutableListOf()
	}
}


