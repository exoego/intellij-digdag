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

val KNOWN_EXTENSIONS = setOf("yml", "yaml", "sql", "dig", "txt", "csv")

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

        val textValue = when (element) {
            is DigdagQuotedTextImpl -> element.getTextValue()
            is DigdagPlainTextImpl -> element.getTextValue()
            else -> return PsiReference.EMPTY_ARRAY
        }

        // TODO: special support for python and ruby
        if (!KNOWN_EXTENSIONS.contains(textValue.substringAfterLast(".").lowercase())) return PsiReference.EMPTY_ARRAY

        return arrayOf(FileReference(element, textRange, element.containingFile))
    }
}

internal class FileReference(
    element: PsiElement,
    textRange: TextRange,
    private val baseFile: PsiFile,
) : PsiReferenceBase<PsiElement>(element, textRange) {
    private val key = element.text.substring(textRange.startOffset, textRange.endOffset)

    override fun resolve(): PsiElement? {
        val project = myElement.project
        val parentDir = baseFile.virtualFile.parent
        return parentDir.findFileByRelativePath(key)?.findPsiFile(project)
    }
}
