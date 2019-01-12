package main.kotlin.knowledgebase

import net.sf.tweety.logics.fol.syntax.FOLAtom

data class FOLLiteral(val atom : FOLAtom, val neg : Boolean) : Comparable<FOLLiteral> {
	override fun compareTo(other: FOLLiteral): Int {
		return this.toString().compareTo(other.toString())
	}

	fun not() : FOLLiteral{
		return FOLLiteral(atom, !neg)
	}
	override fun toString() : String {
		if(neg){
			return "!" + atom.toString()
		}
		return atom.toString()
	}


}