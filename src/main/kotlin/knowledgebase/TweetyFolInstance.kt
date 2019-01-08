package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.reasoner.EFOLReasoner
import net.sf.tweety.logics.fol.reasoner.FolReasoner
import net.sf.tweety.logics.fol.reasoner.NaiveFolReasoner
import net.sf.tweety.logics.fol.syntax.*


class TweetyFolInstance(val parser : FolParser, val beliefSet: FolBeliefSet) : Instance {
	companion object {
		init {
			FolReasoner.setDefaultReasoner(NaiveFolReasoner());
		}
	}
	override fun infer(query : Formula, rules : Set<InferenceRule>, inferenceDepth : Int): Map<Set<InferenceRule>, EvidenceInterval> {
		val count = this.count(query)
		val depth = if(inferenceDepth > rules.size) rules.size else inferenceDepth
		//If we aren't inferring or we know the answer, just return the raw truth value
		if (depth == 0 || Math.abs(count.correlation()) == 1.0) {
			return mapOf(Pair(setOf(), count))
		}
		var possibleIntervals = mutableMapOf<Set<InferenceRule>, EvidenceInterval>(Pair(setOf(), count))
		//It's unknown. Time for inference!
		for (rule in rules) {
			//Pretend the rule is true (if correlation > 0) or false (if correlation < 0)
			val folFormula = rule.formula as FolFormula
			var quantified = ForallQuantifiedFormula(folFormula, folFormula.unboundVariables)
			val correlation = rule.evidence.correlation()
			if(correlation > 0){
				beliefSet.add(quantified)
			}
			else if(correlation < 0){
				beliefSet.add(Negation(quantified))
			}
			else {
				//Skip it.
				continue
			}
			//Now, see if we learn anything by assuming it's there.
			val conf = infer(query, rules, depth - 1)
			//Add the possible evidence intervals to it.
			conf.forEach {
				val rulesUsed = mutableSetOf(rule)
				rulesUsed.addAll(it.key)
				possibleIntervals.put(rulesUsed, it.value.intersection(rule.evidence))
			}
			//Remove it so that we don't use it in the next pass.
			if(correlation > 0){
				beliefSet.remove(quantified)
			}
			else if(correlation < 0){
				beliefSet.remove(Negation(quantified))
			}
		}
		return possibleIntervals.toMap()
	}
	override fun query(query: Formula): TruthValue {
		val f = query as FolFormula;
		val reasoner = FolReasoner.getDefaultReasoner()
		if(reasoner.query(beliefSet, f)){
			return TruthValue.TRUE
		}
		if(reasoner.query(beliefSet, Negation(f))){
			return TruthValue.FALSE
		}
		return TruthValue.UNKNOWN
	}
	override fun count(query : Formula) : EvidenceInterval{

		if(query is FolFormula){
			if(query.unboundVariables.isEmpty()){
				return this.query(query).toConfidenceMeasure(1.0)
			}
			val v = query.unboundVariables.iterator().next()
			val remainingVariables = query.unboundVariables
			remainingVariables.remove(v)
			val constants = v.sort.getTerms(Constant::class.java)
			var builtInterval = EvidenceInterval(0, 0, 0)
			for (c in constants) {
				val sat = (this.count(query.formula.substitute(v, c)))
				builtInterval = builtInterval.add(sat)
			}
			return builtInterval
		}
		throw IllegalArgumentException("Not a valid FOL formula!")
	}

	fun query(queryStr : String) : TruthValue{
		return query(parser.parseFormula(queryStr))
	}

	fun count(queryStr : String) : EvidenceInterval {
		return count(parser.parseFormula(queryStr))
	}
	fun infer(queryStr: String, rules : Set<InferenceRule>, inferenceDepth: Int = 1) : Map<Set<InferenceRule>, EvidenceInterval>{
		return infer(parser.parseFormula(queryStr), rules, inferenceDepth)
	}
	override fun objects(): Set<String> {
		return parser.signature.constants.map {
			it.get()
		}.toSet()
	}


}
