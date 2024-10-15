package net.exoego.intellij.digdag.psi

import com.intellij.psi.PsiElement

interface DigdagValue : DigdagPsiElement {
    fun getTag(): PsiElement?
}