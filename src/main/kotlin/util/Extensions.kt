package main.kotlin.util

import net.sf.tweety.math.probability.Probability

operator fun Probability.compareTo(other: Probability?): Int {
	return this.value.compareTo(other!!.value)
}
