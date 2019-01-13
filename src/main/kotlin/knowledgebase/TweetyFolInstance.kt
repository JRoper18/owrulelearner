package main.kotlin.knowledgebase

import main.kotlin.commons.EvidenceInterval
import main.kotlin.commons.InferenceRule
import main.kotlin.commons.Instance
import main.kotlin.commons.TruthValue
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.reasoner.FolReasoner
import net.sf.tweety.logics.fol.reasoner.NaiveFolReasoner
import net.sf.tweety.logics.fol.syntax.*


class TweetyFolInstance(val parser : FolParser, val beliefSet: FolBeliefSet) : Instance<FolFormula> {
	companion object {
		init {
			FolReasoner.setDefaultReasoner(NaiveFolReasoner()) //Am sad. EPROVER, Spass, and Prover9 are all unix-based.
		}
	}
	override fun infer(query : FolFormula, rules : Set<InferenceRule<FolFormula>>, inferenceDepth : Int): Map<Set<InferenceRule<FolFormula>>, EvidenceInterval> {
		val count = this.count(query)
		val depth = if(inferenceDepth > rules.size) rules.size else inferenceDepth
		//If we aren't inferring or we know the answer, just return the raw truth value
		if (depth == 0 || Math.abs(count.correlation()) == 1.0) {
			return mapOf(Pair(setOf(), count))
		}
		var possibleIntervals = mutableMapOf<Set<InferenceRule<FolFormula>>, EvidenceInterval>(Pair(setOf(), count))
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
	override fun query(f: FolFormula): TruthValue {
		val reasoner = FolReasoner.getDefaultReasoner()
		if(reasoner.query(beliefSet, f)){
			return TruthValue.TRUE
		}
		if(reasoner.query(beliefSet, Negation(f))){
			return TruthValue.FALSE
		}
		return TruthValue.UNKNOWN
	}
	override fun count(query : FolFormula) : EvidenceInterval {
		if(query.unboundVariables.isEmpty()){
			return this.query(query).toConfidenceMeasure(1.0)
		}
		val v = query.unboundVariables.iterator().next()
		val remainingVariables = query.unboundVariables
		remainingVariables.remove(v)
		val constants = v.sort.getTerms(Constant::class.java)
		var builtInterval = EvidenceInterval(0, 0, 0)
		for (c in constants) {
			val sat = (this.count(query.formula.substitute(v, c) as FolFormula))
			builtInterval = builtInterval.add(sat)
		}
		return builtInterval
	}

	fun query(queryStr : String) : TruthValue {
		return query(parser.parseFormula(queryStr) as FolFormula)
	}

	fun count(queryStr : String) : EvidenceInterval {
		return count(parser.parseFormula(queryStr) as FolFormula)
	}
	fun infer(queryStr: String, rules : Set<InferenceRule<FolFormula>>, inferenceDepth: Int = 1) : Map<Set<InferenceRule<FolFormula>>, EvidenceInterval>{
		return infer(parser.parseFormula(queryStr) as FolFormula, rules, inferenceDepth)
	}

	override fun size() : Int {
		return parser.signature.constants.size
	}
}
