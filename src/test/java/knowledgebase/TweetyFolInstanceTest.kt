package test.java.knowledgebase

import main.kotlin.knowledgebase.TruthValue
import main.kotlin.knowledgebase.TweetyFolInstance
import net.sf.tweety.logics.commons.syntax.Constant
import net.sf.tweety.logics.commons.syntax.Predicate
import net.sf.tweety.logics.fol.parser.FolParser
import net.sf.tweety.logics.fol.syntax.FolFormula
import net.sf.tweety.logics.fol.syntax.FolSignature
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TweetyFolInstanceTest {

	@Test
	fun testQuery() {
		val sig = FolSignature(true)
		sig.add(Constant("apple"))
		sig.add(Constant("banana"))
		sig.add(Predicate("isRed", 1))
		sig.add(Predicate("isApple", 1))
		val parser = FolParser()
		parser.signature = sig
		val f1 = parser.parseFormula("isRed(apple)") as FolFormula
		val f2 = parser.parseFormula("!isRed(banana)") as FolFormula
		val f3 = parser.parseFormula("isApple(apple)") as FolFormula
		val f4 = parser.parseFormula("!isApple(banana)") as FolFormula
		val i1 = TweetyFolInstance(parser)
		i1.addFormulas(setOf(f1, f2, f3, f4))
		val i2 = TweetyFolInstance(parser);
		i2.addFormulas(setOf(f1, f3, f4))
		val i3 = TweetyFolInstance(parser)
		i3.addFormulas(setOf(f3, f4))
		val i4 = TweetyFolInstance(parser)
		i4.addFormulas(setOf(f2, f3, f4))
		assertEquals(TruthValue.TRUE, i1.query(parser.parseFormula("forall X: (isApple(X) => isRed(X))")))
		assertEquals(TruthValue.TRUE, i2.query(parser.parseFormula("forall X: (isApple(X) => isRed(X))")))
		assertEquals(TruthValue.UNKNOWN, i3.query(parser.parseFormula("forall X: (isApple(X) => isRed(X))")))
		assertEquals(TruthValue.UNKNOWN, i4.query(parser.parseFormula("forall X: (isApple(X) => isRed(X))")))

		//Let's try some other logical operators in our queries.
		assertEquals(TruthValue.TRUE, i1.query("isApple(apple) && !isApple(banana)"))
		assertEquals(TruthValue.FALSE, i1.query("isApple(apple) && isApple(banana)"))

		assertEquals(TruthValue.UNKNOWN, i2.query("isRed(apple) && isRed(banana)")) //Don't know the color of banana in i2

		assertEquals(TruthValue.TRUE, i2.query("isRed(apple) || isRed(banana)"))
		assertEquals(TruthValue.UNKNOWN, i3.query("isRed(apple) || isRed(banana)"))

		assertEquals(TruthValue.TRUE, i1.query("exists X: (!isRed(X))"))
		assertEquals(TruthValue.UNKNOWN, i2.query("exists X: (!isRed(X))"))
		assertEquals(TruthValue.UNKNOWN, i3.query("exists X: (!isRed(X))"))


		//HOW DO WE DO THIS, IT RETURNS UNKNOWN: assertEquals(TruthValue.TRUE, i2.query("isRed(banana) || !isRed(banana)"))
	}

	@Test
	fun testObjects() {
		//TODO: Write tests for this
	}
}