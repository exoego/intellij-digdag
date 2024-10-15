package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor

class DigdagBlockSequenceImpl(node: ASTNode) : DigdagSequenceImpl(node) {
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitSequence(this)
        } else {
            super.accept(visitor)
        }
    }
}
