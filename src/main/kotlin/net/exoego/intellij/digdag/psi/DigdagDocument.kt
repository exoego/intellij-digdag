package net.exoego.intellij.digdag.psi

interface DigdagDocument : DigdagPsiElement {
    fun getTopLevelValue(): DigdagValue?
}