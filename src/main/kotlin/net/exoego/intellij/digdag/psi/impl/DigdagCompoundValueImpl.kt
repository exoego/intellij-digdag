package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagCompoundValue
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagScalar

open class DigdagCompoundValueImpl(node: ASTNode) : DigdagValueImpl(node), DigdagCompoundValue {
    override fun getTextValue(): String {
        var element = if (getTag() != null) getTag()!!.nextSibling else firstChild

        while (element != null && element !is DigdagScalar) {
            element = element.nextSibling
        }

        return if (element != null) {
            (element as DigdagScalar).getTextValue()
        } else {
            "<compoundValue:${Integer.toHexString(text.hashCode())}>"
        }
    }


    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitCompoundValue(this)
        } else {
            super.accept(visitor)
        }
    }

    override fun toString(): String = "Digdag compound value"
}