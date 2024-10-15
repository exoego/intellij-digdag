package net.exoego.intellij.digdag.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import net.exoego.intellij.digdag.psi.DigdagPsiElement

open class DigdagPsiElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), DigdagPsiElement {
    override fun toString(): String = "Digdag element"
}
