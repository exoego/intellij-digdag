package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagValue

abstract class DigdagValueImpl(node: ASTNode) : DigdagPsiElementImpl(node), DigdagValue{
    override fun getTag(): PsiElement? {
//        val firstChild = firstChild
//        return if (firstChild.node.elementType === DigdagTokenTypes.TAG) {
//            firstChild
//        } else {
//            null
//        }
        return null
    }

    override fun toString(): String {
        return "Digdag value"
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitValue(this)
        } else {
            super.accept(visitor)
        }
    }
}