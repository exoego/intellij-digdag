package net.exoego.intellij.digdag.stubs

import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import net.exoego.intellij.digdag.DigdagLanguage
import net.exoego.intellij.digdag.DigdagModule

class DigdagModuleStub(module: DigdagModule) : PsiFileStubImpl<DigdagModule>(module) {
    object Type : IStubFileElementType<DigdagModuleStub>(DigdagLanguage) {
        override fun getExternalId(): String = "Digdag.file"
    }
}
