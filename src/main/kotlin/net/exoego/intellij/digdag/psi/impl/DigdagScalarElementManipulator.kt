package net.exoego.intellij.digdag.psi.impl

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import net.exoego.intellij.digdag.DigdagElementGenerator
import net.exoego.intellij.digdag.psi.DigdagScalar

class DigdagScalarElementManipulator : AbstractElementManipulator<DigdagScalarImpl>() {
    override fun getRangeInElement(element: DigdagScalarImpl): TextRange {
        val ranges: List<TextRange> = element.getContentRanges()
        if (ranges.isEmpty()) {
            return TextRange.EMPTY_RANGE
        }
        return TextRange.create(ranges[0].startOffset, ranges[ranges.size - 1].endOffset)
    }

    @Throws(IncorrectOperationException::class)
    override fun handleContentChange(
        element: DigdagScalarImpl,
        range: TextRange,
        newContent: String
    ): DigdagScalarImpl? {
        try {
            val encodeReplacements: List<Pair<TextRange, String>> = element.getEncodeReplacements(newContent)
            val builder = StringBuilder()
            val oldText: String = element.getText()

            builder.append(oldText.subSequence(0, range.startOffset))
            builder.append(DigdagScalarImpl.processReplacements(newContent, encodeReplacements))
            builder.append(oldText.subSequence(range.endOffset, oldText.length))

            val dummyDigdagFile =
                DigdagElementGenerator.getInstance(element.getProject()).createDummyDigdagWithText(builder.toString())
            val newScalar =
                PsiTreeUtil.collectElementsOfType(dummyDigdagFile, DigdagScalar::class.java).iterator().next()

            val result: PsiElement = element.replace(newScalar) as? DigdagScalarImpl
                ?: throw AssertionError("Inserted Digdag scalar, but it isn't a scalar after insertion :(")


            // it is a hack to preserve the `QUICK_EDIT_HANDLERS` key,
            // actually `element.replace` should have done it, but for some reason didn't
            (element.getNode() as UserDataHolderBase).copyCopyableDataTo(result.node as UserDataHolderBase)
            CodeEditUtil.setNodeGenerated(result.node, true)

            return (result as DigdagScalarImpl)
        } catch (e: IllegalArgumentException) {
            val newElement: PsiElement =
                element.replace(
                    DigdagElementGenerator.getInstance(element.getProject()).createDigdagDoubleQuotedString()
                ) as? DigdagQuotedTextImpl
                    ?: throw AssertionError("Could not replace with dummy scalar")
            return handleContentChange(newElement as DigdagScalarImpl, newContent)
        }
    }
}