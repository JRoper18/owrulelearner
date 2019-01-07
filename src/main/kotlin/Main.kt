package main.kotlin

import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.syntax.FolSignature
import net.sf.tweety.logics.fol.syntax.FolBeliefSet
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.reasoner.FolReasoner
import net.sf.tweety.logics.fol.syntax.FolFormula


fun main(args: Array<String>){

	val sig = FolSignature(true) //Create new FOLSignature with equality
	sig.add(Constant("a"))
	sig.add(Predicate("pred", 1))
	val parser = FolParser()
	parser.signature = sig //Use the signature defined above
	val bs = FolBeliefSet()
	bs.add(parser.parseFormula("pred(a)") as FolFormula)
	bs.add(parser.parseFormula("!pred(a)") as FolFormula)


}