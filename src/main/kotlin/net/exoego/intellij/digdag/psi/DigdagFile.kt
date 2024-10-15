package net.exoego.intellij.digdag.psi

import com.intellij.psi.PsiFile

interface DigdagFile : PsiFile, DigdagPsiElement{
    fun getDocuments(): List<DigdagDocument>
}