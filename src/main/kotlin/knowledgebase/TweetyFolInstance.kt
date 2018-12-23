package main.kotlin.knowledgebase

import net.sf.tweety.commons.Formula
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.reasoner.FolReasoner
import net.sf.tweety.logics.fol.syntax.FolBeliefSet
import net.sf.tweety.logics.fol.syntax.FolFormula

class TweetyFolInstance(val parser : FolParser, val beliefSet : FolBeliefSet) : Instance {
	override fun infer(query : Formula, rules : Set<InferenceRule>, inferenceDepth : Int): Map<Set<InferenceRule>, ConfidenceInterval> {
		val rawTruth = this.query(query)
		//If we aren't inferring or we know the answer, just return the raw truth value
		if(inferenceDepth == 0 || rawTruth != TruthValue.UNKNOWN){
			return mapOf(Pair(setOf(), rawTruth.toConfidenceMeasure(1)))
		}
		var possibleIntervals = mutableMapOf<Set<InferenceRule>, ConfidenceInterval>()
		//It's unknown. Time for inference!
		for(rule in rules){
			//Pretend the rule is true.
			val folFormula = rule.formula as FolFormula
			beliefSet.add(folFormula)
			//Now, see if we learn anything by assuming it's there.
			val conf = infer(query, rules, inferenceDepth - 1)
			//Add the possible confidence intervals to it.
			conf.forEach {
				val rulesUsed = mutableSetOf(rule)
				rulesUsed.addAll(it.key)
				possibleIntervals.put(rulesUsed, it.value)
			}
			//Remove it so that we don't use it in the next pass.
			beliefSet.remove(folFormula)
		}
		return possibleIntervals.toMap()
	}

	override fun query(query: Formula): TruthValue {
		val res = FolReasoner.getDefaultReasoner().query(beliefSet, query as FolFormula?)
		return TruthValue.fromBool(res)
	}
	override fun objects(): Set<String> {
		return parser.signature.constants.map {
			it.get()
		}.toSet()
	}

}
