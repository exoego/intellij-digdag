package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagMapping
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagSequence
import net.exoego.intellij.digdag.psi.DigdagSequenceItem
import net.exoego.intellij.digdag.psi.DigdagValue

class DigdagSequenceItemImpl(node: ASTNode) : DigdagPsiElementImpl(node), DigdagSequenceItem {
    override fun getValue(): DigdagValue? = PsiTreeUtil.findChildOfType(this, DigdagValue::class.java)

    override fun getKeysValues(): Collection<DigdagKeyValue> {
        val mapping = PsiTreeUtil.findChildOfType(this, DigdagMapping::class.java)
        return mapping?.getKeyValues() ?: emptyList()
    }

    override fun getItemIndex(): Int {
        val parent = parent
        if (parent is DigdagSequence) {
            return parent.getItems().indexOf(this)
        }
        return 0
    }


    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitSequenceItem(this)
        } else {
            super.accept(visitor)
        }
    }

    override fun toString(): String = "Digdag sequence item"
}