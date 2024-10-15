package net.exoego.intellij.digdag.psi

import com.intellij.openapi.util.NlsSafe
import com.intellij.pom.PomTarget
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.annotations.Contract

interface DigdagKeyValue : DigdagPsiElement, ContributedReferenceHost, PsiNamedElement, PomTarget {
    @Contract(pure = true)
    fun getKey(): PsiElement?

    @Contract(pure = true)
    fun getKeyText(): @NlsSafe String

    @Contract(pure = true)
    fun getValue(): DigdagValue?

    @Contract(pure = true)
    fun getValueText(): @NlsSafe String

    @Contract(pure = true)
    fun getParentMapping(): DigdagMapping?

    fun setValue(value: DigdagValue)
}
