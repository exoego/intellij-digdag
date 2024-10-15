package net.exoego.intellij.digdag.psi.impl

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.TokenSet
import net.exoego.intellij.digdag.DigdagElementTypes
import net.exoego.intellij.digdag.DigdagFileType
import net.exoego.intellij.digdag.DigdagLanguage
import net.exoego.intellij.digdag.psi.DigdagDocument
import net.exoego.intellij.digdag.psi.DigdagFile

class DigdagFileImpl(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DigdagLanguage), DigdagFile {
    override fun getFileType(): FileType = DigdagFileType

    override fun toString(): String = "Digdag file: $name"

    override fun getDocuments(): List<DigdagDocument> {
        val result: ArrayList<DigdagDocument> = ArrayList()
        for (node in node.getChildren(TokenSet.create(DigdagElementTypes.DOCUMENT))) {
            result.add(node.psi as DigdagDocument)
        }
        return result
    }
}