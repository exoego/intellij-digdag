package net.exoego.intellij.digdag.psi

interface DigdagSequence : DigdagCompoundValue{
    fun getItems(): List<DigdagSequenceItem>
}