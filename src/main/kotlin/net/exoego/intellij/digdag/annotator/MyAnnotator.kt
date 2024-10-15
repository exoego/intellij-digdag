package net.exoego.intellij.digdag.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import kotlin.math.min
import net.exoego.intellij.digdag.DigdagBundle
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.DigdagUtil
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagQuotedText
import net.exoego.intellij.digdag.psi.DigdagSequenceItem
import net.exoego.intellij.digdag.psi.DigdagValue
import net.exoego.intellij.digdag.psi.impl.DigdagBlockMappingImpl
import net.exoego.intellij.digdag.psi.impl.DigdagBlockSequenceImpl
import net.exoego.intellij.digdag.psi.impl.DigdagKeyValueImpl
import net.exoego.intellij.digdag.psi.impl.DigdagPlainTextImpl
import net.exoego.intellij.digdag.psi.impl.DigdagQuotedTextImpl
import net.exoego.intellij.digdag.psi.impl.DigdagScalarTextImpl
import org.jetbrains.annotations.Nls

class MyAnnotator : BaseAnnotator() {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (PsiTreeUtil.findChildrenOfType(element, OuterLanguageElement::class.java).isNotEmpty()) return

        if (element is DigdagKeyValueImpl) {
            if (element.getKeyText() == "!include") {
                val value = element.getValue()
                if (value is DigdagScalarTextImpl  || value is DigdagPlainTextImpl || value is DigdagQuotedTextImpl) {
                    val firstLeaf = TreeUtil.findFirstLeaf(value.node)?.psi ?: return
                    holder.newAnnotation(HighlightSeverity.INFORMATION, DigdagBundle.message("include.found"))
                        .range(TextRange.create(value.startOffset, endOfLine(firstLeaf, value))).create()
                    return
                }
            }
        }

    }


    private fun findNeededParent(element: PsiElement): DigdagKeyValue? = PsiTreeUtil.findFirstParent(element, true) {
        it is DigdagKeyValueImpl
    } as DigdagKeyValue?

}