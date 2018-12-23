package main.kotlin.knowledgebase

enum class TruthValue {
	TRUE,
	FALSE,
	UNKNOWN;

	fun toConfidenceMeasure(evidenceSize : Int) : ConfidenceInterval{
		when(this){
			TRUE -> return ConfidenceInterval(evidenceSize, 0, evidenceSize)
			FALSE -> return ConfidenceInterval(0, evidenceSize, evidenceSize)
			UNKNOWN -> return ConfidenceInterval(0, 0, evidenceSize)
		}
	}

	companion object {
		fun fromBool(bool : Boolean?) : TruthValue{
			if(bool == null){
				return UNKNOWN
			}
			return if(bool) TRUE else FALSE
		}
	}

}