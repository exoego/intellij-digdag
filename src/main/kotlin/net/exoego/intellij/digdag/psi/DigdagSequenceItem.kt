package net.exoego.intellij.digdag.psi

interface DigdagSequenceItem : DigdagPsiElement{
    fun getValue(): DigdagValue?

    fun getKeysValues(): Collection<DigdagKeyValue>

    fun getItemIndex(): Int
}
