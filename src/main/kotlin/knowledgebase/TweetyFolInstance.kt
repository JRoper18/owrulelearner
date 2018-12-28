package main.kotlin.knowledgebase

import net.sf.tweety.commons.BeliefBase
import net.sf.tweety.commons.Formula
import net.sf.tweety.commons.Parser
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.reasoner.FolReasoner
import net.sf.tweety.logics.fol.reasoner.NaiveFolReasoner
import net.sf.tweety.logics.fol.syntax.*
import java.io.IOException
import java.io.UncheckedIOException

class TweetyFolInstance(val parser : FolParser) : Instance {
	private val positives = mutableSetOf<FolFormula>()
	private val negatives = mutableSetOf<FolFormula>()
	private val beliefSet = FolBeliefSet()
	override fun infer(query : Formula, rules : Set<InferenceRule>, inferenceDepth : Int): Map<Set<InferenceRule>, ConfidenceInterval> {
		val count = this.count(query)
		//If we aren't inferring or we know the answer, just return the raw truth value
		if (inferenceDepth == 0 || count.correlation() == 1.0) {
			return mapOf(Pair(setOf(), count))
		}
		var possibleIntervals = mutableMapOf<Set<InferenceRule>, ConfidenceInterval>(Pair(setOf(), count))
		//It's unknown. Time for inference!
		for (rule in rules) {
			//Pretend the rule is true (if correlation > 0) or false (if correlation < 0)
			val folFormula = rule.formula as FolFormula
			val correlation = rule.confidence.correlation()
			if(correlation > 0){
				positives.add(folFormula)
			}
			else if(correlation < 0){
				negatives.add(folFormula)
			}
			else {
				//Skip it.
				continue
			}
			//Now, see if we learn anything by assuming it's there.
			val conf = infer(query, rules, inferenceDepth - 1)
			//Add the possible confidence intervals to it.
			conf.forEach {
				val rulesUsed = mutableSetOf(rule)
				rulesUsed.addAll(it.key)
				possibleIntervals.put(rulesUsed, it.value)
			}
			//Remove it so that we don't use it in the next pass.
			if(correlation > 0){
				positives.remove(folFormula)
			}
			else if(correlation < 0){
				negatives.remove(folFormula)
			}
		}
		return possibleIntervals.toMap()
	}
	override fun query(query: Formula): TruthValue {
		val f = query as FolFormula;
		if (!f.isWellFormed)
			throw IllegalArgumentException("The given formula $f is not well-formed.")
		if (!f.isClosed)
			throw IllegalArgumentException("The given formula $f is not closed.")
		//First try to reason with the CWA
		val cwaQuery = NaiveFolReasoner().query(beliefSet, f)
		if(cwaQuery){
			return TruthValue.TRUE
		}
		//False under CWA, but could be unknown without it. Check:
		return satisfies(query)
	}
	fun satisfies(formula: FolFormula?): TruthValue {
		val f = formula as FolFormula
		if(positives.contains(f)){
			return TruthValue.TRUE
		}
		else if(negatives.contains(f)){
			return TruthValue.FALSE
		}
		if (!f.isClosed) throw IllegalArgumentException("FolFormula $f is not closed.")

		if (f is Tautology) {
			return TruthValue.TRUE
		}
		if (f is Contradiction) {
			return TruthValue.FALSE
		}
		if (f is FOLAtom) {
			val p = f.predicate
			if (p is EqualityPredicate) {
				val terms = f.arguments
				return if (terms[0].equals(terms[1]))
					TruthValue.TRUE
				else
					TruthValue.FALSE
			} else if (p is InequalityPredicate) {
				val terms = f.arguments
				return if (terms[0].equals(terms[1]))
					TruthValue.FALSE
				else
					TruthValue.TRUE
			}
			//HERE'S THE IMPORTANT PART THAT MAKES IT THE OWA:
			//Previously: return this.contains(f)
			if(positives.contains(f)){
				return TruthValue.TRUE
			}
			else if(negatives.contains(f)){
				return TruthValue.FALSE
			}
			return TruthValue.UNKNOWN
		}
		//Disjunction and Conjunction follow Kleene/Priest trinary logic.
		if (f is Disjunction) {
			val d = f
			var foundUnknown = false
			for (rf in d){
				val satisfies = (satisfies(rf as FolFormula))
				if(satisfies == TruthValue.TRUE){
					return TruthValue.TRUE
				}
				else if(satisfies == TruthValue.UNKNOWN){
					foundUnknown = true
				}
			}
			if(foundUnknown){
				return TruthValue.UNKNOWN
			}
			return TruthValue.FALSE
		}
		if (f is Conjunction) {
			val c = f
			var foundUnknown = false

			for (rf in c){
				val satisfies = (satisfies(rf as FolFormula))
				if(satisfies == TruthValue.FALSE){
					return TruthValue.FALSE;
				}
				else if(satisfies == TruthValue.UNKNOWN){
					foundUnknown = true
				}
			}
			if(foundUnknown){
				return TruthValue.UNKNOWN;
			}
			return TruthValue.TRUE
		}
		//Implication also Kleene/Priest rather than Lukaseiwicz, because OWA: We don't assume what we have no data for.
		if (f is Implication) {
			val firstSat = this.satisfies(f.formulas.first as FolFormula)
			val secondSat = this.satisfies(f.formulas.second as FolFormula)
			if(firstSat == TruthValue.FALSE || secondSat == TruthValue.TRUE){
				return TruthValue.TRUE; //False implies true.
			}
			if (firstSat == TruthValue.TRUE && secondSat == TruthValue.FALSE){
				return TruthValue.FALSE;
			}
			return TruthValue.UNKNOWN
		}
		if (f is Equivalence) {
			val e = f
			val a = e.getFormulas().getFirst()
			val b = e.getFormulas().getSecond()
			val firstSat = this.satisfies(a as FolFormula)
			val secondSat = this.satisfies(b as FolFormula)
			if (firstSat == TruthValue.UNKNOWN || secondSat == TruthValue.UNKNOWN) {
				return TruthValue.UNKNOWN
			}
			return TruthValue.fromBool(firstSat == secondSat)
		}
		if (f is Negation) {
			val n = f
			return this.satisfies(n.getFormula()).not()
		}
		if (f is ExistsQuantifiedFormula) {
			if (f.quantifierVariables.isEmpty()) return this.satisfies(f.formula)
			val v = f.quantifierVariables.iterator().next()
			val remainingVariables = f.quantifierVariables
			remainingVariables.remove(v)

			val constants = v.sort.getTerms(Constant::class.java)
			var foundUnknown = false;
			if (remainingVariables.isEmpty()) {
				for (c in constants) {
					val satisfies = satisfies(f.formula.substitute(v, c))
					if (satisfies == TruthValue.TRUE)
						return TruthValue.TRUE
					else if(satisfies == TruthValue.UNKNOWN){
						foundUnknown = true;
					}
				}

			} else {
				for (c in constants) {
					val satisfies = satisfies(ExistsQuantifiedFormula(f.formula.substitute(v, c), remainingVariables))
					if(satisfies == TruthValue.TRUE)
						return TruthValue.TRUE
					else if(satisfies == TruthValue.UNKNOWN){
						foundUnknown = true
					}
				}
			}
			if(foundUnknown){ //If there's a chance one of the existing things fill this property but we don't know, we don't know.
				return TruthValue.UNKNOWN
			}
			return TruthValue.FALSE
		}
		if (f is ForallQuantifiedFormula) {
			if (f.quantifierVariables.isEmpty()) return this.satisfies(f.formula)
			val v = f.quantifierVariables.iterator().next()
			val remainingVariables = f.quantifierVariables
			remainingVariables.remove(v)

			val constants = v.sort.getTerms(Constant::class.java)
			var foundUnknown = false;
			for (c in constants) {
				val sat = (this.satisfies(ForallQuantifiedFormula(f.formula.substitute(v, c), remainingVariables)))
				if(sat == TruthValue.FALSE){
					return TruthValue.FALSE
				}
				else if(sat == TruthValue.UNKNOWN){
					foundUnknown = true
				}
			}
			if(foundUnknown){ //If we aren't sure it holds for EVERY one, we aren't sure.
				return TruthValue.UNKNOWN
			}
			return TruthValue.TRUE
		}
		throw IllegalArgumentException("FolFormula $f is of unknown type.")
	}
	fun satisfies(beliefSet: FolBeliefSet) : TruthValue {
		var foundUnknown = false
		beliefSet.forEach {
			val sat = this.satisfies(it)
			if(sat == TruthValue.FALSE){
				return TruthValue.FALSE
			}
			else if(sat == TruthValue.UNKNOWN){
				foundUnknown = true
			}
		}
		if(foundUnknown){
			return TruthValue.UNKNOWN
		}
		return TruthValue.TRUE
	}

	override fun count(query : Formula) : ConfidenceInterval{
		if(query is FolFormula){
			if(query is ForallQuantifiedFormula){ //A forall formula indicates that we want to count how many times it's true.
				if (query.quantifierVariables.isEmpty()) return this.count(query.formula as FolFormula)
				val v = query.quantifierVariables.iterator().next()
				val remainingVariables = query.quantifierVariables
				remainingVariables.remove(v)
				val constants = v.sort.getTerms(Constant::class.java)
				var builtInterval = ConfidenceInterval(0, 0, 0)
				for (c in constants) {
					val sat = (this.count(ForallQuantifiedFormula(query.formula.substitute(v, c), remainingVariables)))
					builtInterval = builtInterval.add(sat)
				}
				return builtInterval
			}
			else if(query is Implication){
				val firstSat =(query(query.formulas.first))
				val secondSat = query(query.formulas.second)
				if(firstSat == TruthValue.FALSE){
					return ConfidenceInterval(0, 0, 0)
				}
				else if(firstSat == TruthValue.UNKNOWN || secondSat == TruthValue.UNKNOWN){
					return ConfidenceInterval(0, 0, 1) //TODO: How do I deal with an uncertain antecedent?
				}
				if(secondSat == TruthValue.FALSE){
					return ConfidenceInterval(0, 1, 1)
				}
				return ConfidenceInterval(1, 0, 1)

			}
			return query(query).toConfidenceMeasure(1.0)
		}
		throw IllegalArgumentException("Not a valid formula!")
	}

	fun query(queryStr : String) : TruthValue{
		return query(parser.parseFormula(queryStr))
	}

	fun count(queryStr : String) : ConfidenceInterval {
		return count(parser.parseFormula(queryStr))
	}
	fun infer(queryStr: String, rules : Set<InferenceRule>, inferenceDepth: Int = 1) : Map<Set<InferenceRule>, ConfidenceInterval>{
		return infer(parser.parseFormula(queryStr), rules, inferenceDepth)
	}
	override fun objects(): Set<String> {
		return parser.signature.constants.map {
			it.get()
		}.toSet()
	}
	fun addFormula(formula : FolFormula, positive : Boolean = true){
		if(formula is Negation){
			addFormula(formula.formula, !positive)
		}
		else if(formula is Conjunction){
			formula.forEach {
				addFormula(it as FolFormula, positive)
			}
			addFormula(formula, positive)
		}
		else{
			if(positive){
				beliefSet.add(formula)
				positives.add(formula)
			}
			else {
				beliefSet.add(Negation(formula))
				negatives.add(formula)
			}
		}
	}
	fun addFormulas(formulas : Set<FolFormula>){
		formulas.forEach{
			addFormula(it)
		}
	}

}
