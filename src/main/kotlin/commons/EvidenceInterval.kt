package main.kotlin.commons

import net.sf.tweety.math.Interval
import net.sf.tweety.math.probability.Probability
import java.lang.IllegalArgumentException
import kotlin.math.max

data class EvidenceInterval(val positive : Double, val negative : Double, val total : Double) {
	constructor(positive : Int, negative : Int, total : Int) : this(positive + 0.0, negative + 0.0, total + 0.0)
	init {
		if(positive < 0 || negative < 0 || total < 0){
			//Not valid, positive reals ONLY.
			throw IllegalArgumentException("EvidenceIntervals can only have positive (>=0) values.")
		}
		else if(positive + negative > total){
			//Also not valid.
			throw IllegalArgumentException("The number of positive and negative proof must total less than or equal to the total. ($positive+$negative<=$total")
		}
	}
	fun correlation() : Double {
		if(total == 0.0){
			return 0.0
		}
		val res = (positive - negative) / total
		return res
	}
	fun minCorrelation() : Double {
		return (positive + unknown() - negative) / total
	}
	fun maxCorrelation() : Double {
		return (positive - negative - unknown()) / total
	}

	fun evidence() : Double {
		return positive + negative
	}
	fun scale(scalar : Double) : EvidenceInterval {
		return EvidenceInterval(positive * scalar, negative * scalar, total * scalar)
	}
	fun normalize(toVal : Double = 1.0) : EvidenceInterval {
		return scale(toVal / total)
	}
	fun add(interval : EvidenceInterval) : EvidenceInterval {
		return EvidenceInterval(positive + interval.positive, negative + interval.negative, total + interval.total)
	}
	fun negation() : EvidenceInterval {
		return EvidenceInterval(negative, positive, total)
	}

	fun intersection(other : EvidenceInterval, intersectionSize : Double = 1.0) : EvidenceInterval {
		//NOTE: These calculations are based off of intuitionistic fuzzy set theory, a good paper on which is here:
		//https://www.irit.fr/~Didier.Dubois/Papers/cloudeus.pdf
		//Scale down each interval to the unit interval by dividing by total, and then scale them back up again.
		val numPositive = Math.min((positive / total), (other.positive / other.total)) * intersectionSize
		val numNegative = Math.max(negative / total, other.negative / other.total) * intersectionSize
		return EvidenceInterval(numPositive, numNegative, intersectionSize)
	}

	/**
	 * Takes the union of this interval and another interval, assuming maximum number of positives are joined.
	 */
	fun union(other : EvidenceInterval, unionSize : Double = 1.0) : EvidenceInterval {
		//TODO: Figure out a way to find the total number of occurances across instances instead of passing it in as param.
		//NOTE: These calculations are based off of intuitionistic fuzzy set theory, a good paper on which is here:
		//https://www.irit.fr/~Didier.Dubois/Papers/cloudeus.pdf
		//Scale down each interval to the unit interval by dividing by total, and then scale them back up again.
		val numPositive = Math.max((positive / total), (other.positive / other.total)) * unionSize
		val numNegative = Math.min(negative / total, other.negative / other.total) * unionSize
		return EvidenceInterval(numPositive, numNegative, unionSize)
	}

	fun unknown() : Double{
		return total - positive - negative
	}
	fun positiveInterval() : Interval<Double>{
		return Interval(positive, positive + unknown())
	}
	fun negativeInterval() : Interval<Double> {
		return Interval(negative, negative + unknown())
	}
	fun correlationInterval() : Interval<Double> {
		return Interval(minCorrelation(), maxCorrelation())
	}
	fun probabilityInterval() : Interval<Probability> {
		return Interval(Probability(positive / total), Probability((positive + unknown()) / total))
	}
	/**
	 *	Returns a truth-value simplification of this evidence measure.
	 *	Skepticism is the minimum amount of correlation needed
	 *	MinEvidence is the mimimum amount of evidence needed
	 */
	fun toTruthValue(skepticism: Double, minEvidence : Double) : TruthValue {
		if(Math.abs(correlation()) < skepticism || evidence() < minEvidence){
			return TruthValue.UNKNOWN
		}
		else {
			return if(correlation() > 0) TruthValue.TRUE else if(correlation() < 0) TruthValue.FALSE else TruthValue.UNKNOWN
		}
	}

	override fun toString() : String {
		return "(P=$positive, N=$negative, T=$total)"
	}


	companion object {
		val EMPTY = EvidenceInterval(0, 0, 0)
		val POSITIVE = EvidenceInterval(1, 0, 1)
		val NEGATIVE = EvidenceInterval(0, 1, 1)
		val UNKNOWN = EvidenceInterval(0, 0, 1)
	}
}