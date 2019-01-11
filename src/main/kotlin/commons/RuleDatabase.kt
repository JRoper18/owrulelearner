package main.kotlin.commons

import net.sf.tweety.commons.Formula

open class RuleDatabase<T : Formula>(val assumptionsToRule: MutableMap<Set<T>, T>){
}