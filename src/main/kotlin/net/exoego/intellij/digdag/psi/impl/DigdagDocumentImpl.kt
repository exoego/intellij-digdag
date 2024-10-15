package net.exoego.intellij.digdag.psi.impl

import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon
import net.exoego.intellij.digdag.DigdagBundle
import net.exoego.intellij.digdag.psi.DigdagDocument
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagValue

class DigdagDocumentImpl(node: ASTNode) : DigdagPsiElementImpl(node), DigdagDocument{
    override fun toString(): String = "Digdag document"

    override fun getTopLevelValue(): DigdagValue? = PsiTreeUtil.findChildOfType(this, DigdagValue::class.java)

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitDocument(this)
        } else {
            super.accept(visitor)
        }
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String = DigdagBundle.message("element.presentation.document.type")

            override fun getLocationString(): String = containingFile.name

            override fun getIcon(unused: Boolean): Icon = AllIcons.Json.Object
        }
    }
}