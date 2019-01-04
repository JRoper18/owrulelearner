package main.kotlin.knowledgebase

data class ConfidenceInterval(val positive : Double, val negative : Double, val total : Double) {
	constructor(positive : Int, negative : Int, total : Int) : this(positive + 0.0, negative + 0.0, total + 0.0)
	fun correlation() : Double {
		return (positive - negative) / total
	}
	fun evidence() : Double {
		return positive + negative
	}
	fun scale(scalar : Double) : ConfidenceInterval{
		return ConfidenceInterval(positive * scalar, negative * scalar, total * scalar)
	}
	fun normalize(toVal : Double) : ConfidenceInterval {
		return scale(toVal / total)
	}
	fun normalize() : ConfidenceInterval {
		return normalize(1.0)
	}
	fun add(interval : ConfidenceInterval) : ConfidenceInterval{
		return ConfidenceInterval(positive + interval.positive, negative + interval.negative, total + interval.total)
	}
	fun negation() : ConfidenceInterval {
		return ConfidenceInterval(negative, positive, total)
	}

	fun intersection(other : ConfidenceInterval, intersectionSize : Double) : ConfidenceInterval{
		//NOTE: These calculations are based off of intuitionistic fuzzy set theory, a good paper on which is here:
		//https://www.irit.fr/~Didier.Dubois/Papers/cloudeus.pdf
		//Scale down each interval to the unit interval by dividing by total, and then scale them back up again.
		val numPositive = Math.min((positive / total), (other.positive / other.total)) * intersectionSize
		val numNegative = Math.max(negative / total, other.negative / other.total) * intersectionSize
		return ConfidenceInterval(numPositive, numNegative, intersectionSize)
	}

	/**
	 * Takes the union of this interval and another interval, assuming maximum number of positives are joined.
	 */
	fun union(other : ConfidenceInterval, unionSize : Double) : ConfidenceInterval{
		//TODO: Figure out a way to find the total number of occurances across instances instead of passing it in as param.
		//NOTE: These calculations are based off of intuitionistic fuzzy set theory, a good paper on which is here:
		//https://www.irit.fr/~Didier.Dubois/Papers/cloudeus.pdf
		//Scale down each interval to the unit interval by dividing by total, and then scale them back up again.
		val numPositive = Math.max((positive / total), (other.positive / other.total)) * unionSize
		val numNegative = Math.min(negative / total, other.negative / other.total) * unionSize
		return ConfidenceInterval(numPositive, numNegative, unionSize)
	}
	/**
	 *	Returns a truth-value simplification of this confidence measure.
	 *	Skepticism is the minimum amount of correlation needed
	 *	MinEvidence is the mimimum amount of evidence needed
	 */
	fun toTruthValue(skepticism: Double, minEvidence : Double) : TruthValue{
		if(Math.abs(correlation()) < skepticism || evidence() < minEvidence){
			return TruthValue.UNKNOWN
		}
		else {
			return if(correlation() > 0) TruthValue.TRUE else if(correlation() < 0) TruthValue.FALSE else TruthValue.UNKNOWN
		}
	}
}