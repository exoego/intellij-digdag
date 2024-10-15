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
import net.exoego.intellij.digdag.psi.DigdagSequenceItem
import net.exoego.intellij.digdag.psi.DigdagValue
import net.exoego.intellij.digdag.psi.impl.DigdagBlockMappingImpl
import net.exoego.intellij.digdag.psi.impl.DigdagBlockSequenceImpl
import net.exoego.intellij.digdag.psi.impl.DigdagKeyValueImpl
import org.jetbrains.annotations.Nls

class DigdagInvalidBlockChildrenErrorAnnotator : BaseAnnotator() {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (PsiTreeUtil.findChildrenOfType(element, OuterLanguageElement::class.java).isNotEmpty()) return

        if (anotherErrorWillBeReported(element)) return

        if (reportSameLineWarning(element, holder)) return

        if (element is DigdagBlockMappingImpl) {
            if (!isValidBlockMapChild(element.firstChild)) {
                reportWholeElementProblem(holder, element, element.getFirstKeyValue().getKey() ?: element.getFirstKeyValue())
                return
            }

            element.children.firstOrNull { !isValidBlockMapChild(it) }?.let {
                reportSubElementProblem(holder, DigdagBundle.message("inspections.invalid.child.in.block.mapping"), it)
            }

            checkIndent(element.getKeyValues().toList(), holder, DigdagBundle.message("inspections.invalid.key.indent"))
        }
        if (element is DigdagBlockSequenceImpl) {
            if (!isValidBlockSequenceChild(element.firstChild)) {
                reportWholeElementProblem(holder, element, element.getItems().firstOrNull() ?: element)
                return
            }

            element.children.firstOrNull { !isValidBlockSequenceChild(it) }?.let {
                reportSubElementProblem(holder, DigdagBundle.message("inspections.invalid.child.in.block.sequence"), it)
            }

            checkIndent(element.getItems(), holder, DigdagBundle.message("inspections.invalid.list.item.indent"))
        }
    }

    private fun reportWholeElementProblem(holder: AnnotationHolder, element: PsiElement, reportElement: PsiElement) {
        holder.newAnnotation(HighlightSeverity.ERROR, getMessageForParent(element))
            .range(TextRange.create(element.startOffset, endOfLine(reportElement, element))).create()
    }

    private fun checkIndent(elements: List<PsiElement>, holder: AnnotationHolder, message: @Nls String) {
        if (elements.size > 1) {
            val firstIndent = DigdagUtil.getIndentToThisElement(elements.first())
            for (item in elements.subList(1, elements.size)) {
                if (DigdagUtil.getIndentToThisElement(item) != firstIndent) {
                    reportSubElementProblem(holder, message, item)
                }
            }
        }
    }

    private fun getMessageForParent(element: PsiElement) =
        if (findNeededParent(element) is DigdagKeyValueImpl)
            DigdagBundle.message("inspections.invalid.child.in.block.mapping")
        else DigdagBundle.message("inspections.invalid.child.in.block.sequence")

    private fun isValidBlockMapChild(element: PsiElement?): Boolean =
        element.let { it is DigdagKeyValue || it is LeafPsiElement }
//        element.let { it is DigdagKeyValue || it is DigdagAnchor || it is LeafPsiElement }

    private fun isValidBlockSequenceChild(element: PsiElement?): Boolean =
        element.let { it is DigdagSequenceItem || it is LeafPsiElement }
//        element.let { it is DigdagSequenceItem || it is DigdagAnchor || it is LeafPsiElement }

    private fun anotherErrorWillBeReported(element: PsiElement): Boolean {
        val kvParent = findNeededParent(element) ?: return false
        val kvGrandParent = kvParent.parentOfType<DigdagKeyValueImpl>(withSelf = false) ?: return false

        return DigdagUtil.psiAreAtTheSameLine(kvGrandParent, element)
    }

    private fun findNeededParent(element: PsiElement) = PsiTreeUtil.findFirstParent(element, true) {
        it is DigdagKeyValueImpl || it is DigdagSequenceItem
    }

    private fun reportSameLineWarning(value: PsiElement, holder: AnnotationHolder): Boolean {
        val keyValue = value.parent
        if (keyValue !is DigdagKeyValue) return false
        val key = keyValue.getKey() ?: return false
        if (value is DigdagBlockMappingImpl) {
            val firstSubValue = value.getFirstKeyValue()
            if (DigdagUtil.psiAreAtTheSameLine(key, firstSubValue)) {
                reportAboutSameLine(holder, value)
                return true
            }
        }
        if (value is DigdagBlockSequenceImpl) {
            val items = value.getItems()
            if (items.isEmpty()) {
                // a very strange situation: a sequence without any item
                return true
            }
            val firstItem = items[0]
            if (DigdagUtil.psiAreAtTheSameLine(key, firstItem)) {
                reportAboutSameLine(holder, value)
                return true
            }
        }
        return false
    }

    private fun reportAboutSameLine(holder: AnnotationHolder, value: DigdagValue) {
        reportSubElementProblem(holder, DigdagBundle.message("annotator.same.line.composed.value.message"), value)
    }

    private fun reportSubElementProblem(holder: AnnotationHolder, message: @Nls String, subElement: PsiElement) {
        val firstLeaf = TreeUtil.findFirstLeaf(subElement.node)?.psi ?: return
        holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(TextRange.create(subElement.startOffset, endOfLine(firstLeaf, subElement))).create()
    }
}