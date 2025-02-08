package net.exoego.intellij.digdag.reference

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.virtualFile
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import net.exoego.intellij.digdag.DigdagElementTypes
import net.exoego.intellij.digdag.DigdagFileType
import net.exoego.intellij.digdag.psi.impl.DigdagPlainTextImpl
import net.exoego.intellij.digdag.psi.impl.DigdagQuotedTextImpl

val TEXT_ELEMENT_TYPES = setOf(
    DigdagElementTypes.SCALAR_TEXT_VALUE,
    DigdagElementTypes.SCALAR_PLAIN_VALUE,
    DigdagElementTypes.SCALAR_QUOTED_STRING,
)

class DigdagFileReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            StandardPatterns.or(
                *TEXT_ELEMENT_TYPES
                    .map { psiElement(it).inVirtualFile(virtualFile().withExtension(DigdagFileType.defaultExtension)) }
                    .toTypedArray<PsiElementPattern.Capture<PsiElement>>()
            ),
            FileReferenceProvider
        )
    }
}

object FileReferenceProvider : PsiReferenceProvider() {
    override fun acceptsTarget(target: PsiElement): Boolean = true

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val escaper = when (element) {
            is DigdagQuotedTextImpl -> element.createLiteralTextEscaper()
            is DigdagPlainTextImpl -> element.createLiteralTextEscaper()
            else -> return PsiReference.EMPTY_ARRAY
        }
        if (!escaper.isOneLine) return PsiReference.EMPTY_ARRAY

        val textRange = escaper.relevantTextRange
        if (textRange.isEmpty) return PsiReference.EMPTY_ARRAY

        val key = element.text.substring(textRange.startOffset, textRange.endOffset)
        val maybeFile = listOf(
            FileReference(element, textRange, key, element.containingFile),
            // Support extension-less file reference for py> and rb> operators
            FileReference(element, textRange, "$key.rb", element.containingFile),
            FileReference(element, textRange, "$key.py", element.containingFile),
        ).firstOrNull { it.resolve() != null }
        if (maybeFile == null) {
            return PsiReference.EMPTY_ARRAY
        }
        return arrayOf(maybeFile)
    }
}

internal class FileReference(
    element: PsiElement,
    textRange: TextRange,
    private val key: String,
    private val baseFile: PsiFile,
) : PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve(): PsiElement? {
        val project = myElement.project
        val parentDir = baseFile.virtualFile.parent
        return parentDir.findFileByRelativePath(key)?.findPsiFile(project)
    }
}
