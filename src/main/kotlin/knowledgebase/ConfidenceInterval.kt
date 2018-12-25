package main.kotlin.knowledgebase

data class ConfidenceInterval(val positive : Double, val negative : Double, val total : Double) {
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
			return if(correlation() > 0) TruthValue.TRUE else TruthValue.FALSE
		}
	}
}