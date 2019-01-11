package main.kotlin.knowledgebase

import main.kotlin.commons.EvidenceInterval
import main.kotlin.commons.RuleDatabase
import net.sf.tweety.logics.fol.syntax.FolFormula

class AssociationRuleDatabase(rulesToEvidence : Map<FolFormula, EvidenceInterval>) : RuleDatabase<FolFormula>(rulesToEvidence)  {

}