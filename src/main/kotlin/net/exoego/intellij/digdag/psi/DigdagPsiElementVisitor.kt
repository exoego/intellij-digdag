package net.exoego.intellij.digdag.psi

import com.intellij.psi.PsiElementVisitor

abstract class DigdagPsiElementVisitor : PsiElementVisitor() {
    fun visitCompoundValue(compoundValue: DigdagCompoundValue) {
        visitValue(compoundValue)
    }

    fun visitDocument(document: DigdagDocument) {
        visitElement(document)
    }

    fun visitKeyValue(keyValue: DigdagKeyValue) {
        visitElement(keyValue)
    }

    fun visitMapping(mapping: DigdagMapping) {
        visitCompoundValue(mapping)
    }

    fun visitSequenceItem(sequenceItem: DigdagSequenceItem) {
        visitElement(sequenceItem)
    }

    fun visitQuotedText(quotedText: DigdagQuotedText) {
        visitScalar(quotedText)
    }

    fun visitScalar(scalar: DigdagScalar) {
        visitValue(scalar)
    }

    fun visitScalarList(scalarList: DigdagScalarList) {
        visitScalar(scalarList)
    }

    fun visitScalarText(scalarText: DigdagScalarText) {
        visitScalar(scalarText)
    }

    fun visitValue(value: DigdagValue) {
        visitElement(value)
    }

    fun visitSequence(sequence: DigdagSequence) {
        visitCompoundValue(sequence)
    }
}