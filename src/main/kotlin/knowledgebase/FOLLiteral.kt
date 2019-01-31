package main.kotlin.knowledgebase

import net.sf.tweety.logics.fol.syntax.FOLAtom
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.Negation

data class FOLLiteral(val atom : FOLAtom, val neg : Boolean) : Comparable<FOLLiteral> {
	override fun compareTo(other: FOLLiteral): Int {
		return this.toString().compareTo(other.toString())
	}
	fun isGround() : Boolean {
		return atom.isGround
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
	fun toFormula() : FolFormula {
		if(neg){
			return Negation(atom)
		}
		return atom
	}
}