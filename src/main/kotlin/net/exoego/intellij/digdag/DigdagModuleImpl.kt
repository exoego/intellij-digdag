package net.exoego.intellij.digdag

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class DigdagModuleImpl(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, DigdagLanguage), DigdagModule {

    override fun getFileType() = DigdagFileType

    override val displayName: String
        get() = viewProvider.virtualFile.nameWithoutExtension
}
