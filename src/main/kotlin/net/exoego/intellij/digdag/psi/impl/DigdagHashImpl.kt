package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import net.exoego.intellij.digdag.DigdagElementGenerator
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.psi.DigdagFile
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagMapping

class DigdagHashImpl(node: ASTNode) : DigdagMappingImpl(node), DigdagMapping {
    override fun addNewKey(key: DigdagKeyValue) {
        var anchor: PsiElement? = null
        var child = lastChild
        while (child != null) {
            val type = child.node.elementType
            if (type === DigdagTokenTypes.COMMA || type === DigdagTokenTypes.LBRACE) {
                anchor = child
            }
            child = child.prevSibling
        }

        addAfter(key, anchor)

        val dummyFile: DigdagFile = DigdagElementGenerator.getInstance(project).createDummyDigdagWithText("{,}")
        val comma: PsiElement? = dummyFile.findElementAt(1)
        assert(comma != null && comma.node.elementType === DigdagTokenTypes.COMMA)

        addAfter(comma!!, key)
    }

    override fun toString(): String = "Digdag hash"
}