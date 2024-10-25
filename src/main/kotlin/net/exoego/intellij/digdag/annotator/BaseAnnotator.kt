package net.exoego.intellij.digdag.annotator

import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.endOffset
import kotlin.math.min
import net.exoego.intellij.digdag.DigdagTokenTypes

abstract class BaseAnnotator : Annotator {
    protected fun endOfLine(subElement: PsiElement, whole: PsiElement): Int {
        var current = subElement
        while (true) {
            val next = PsiTreeUtil.nextLeaf(current) ?: break
            if (PsiUtilCore.getElementType(next) === DigdagTokenTypes.EOL) {
                break
            }
            current = next
            if (current.endOffset >= whole.endOffset) {
                break
            }
        }
        return min(current.endOffset, whole.endOffset)
    }
}