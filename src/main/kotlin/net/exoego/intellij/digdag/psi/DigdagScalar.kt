package net.exoego.intellij.digdag.psi

import com.intellij.psi.PsiLanguageInjectionHost

interface DigdagScalar : DigdagValue, PsiLanguageInjectionHost  {
    fun getTextValue(): String
    fun isMultiline(): Boolean
}
