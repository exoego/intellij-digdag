package net.exoego.intellij.digdag.psi.impl

import com.intellij.psi.PsiElement
import net.exoego.intellij.digdag.DigdagModule

val PsiElement.enclosingModule: DigdagModule?
    get() = containingFile as? DigdagModule
