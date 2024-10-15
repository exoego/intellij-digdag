package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagSequence
import net.exoego.intellij.digdag.psi.DigdagSequenceItem

abstract class DigdagSequenceImpl(node: ASTNode) : DigdagCompoundValueImpl(node), DigdagSequence {
    override fun toString(): String {
        return "Digdag sequence"
    }

    override fun getItems(): List<DigdagSequenceItem> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, DigdagSequenceItem::class.java)
    }

    override fun getTextValue(): String {
        return "<sequence:" + Integer.toHexString(text.hashCode()) + ">"
    }


    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitSequence(this)
        } else {
            super.accept(visitor)
        }
    }
}
