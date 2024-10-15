package net.exoego.intellij.digdag.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import net.exoego.intellij.digdag.DigdagGrammarCharUtil

class DigdagQuotedTextTextEvaluator(text: DigdagQuotedTextImpl) : DigdagScalarTextEvaluator<DigdagQuotedTextImpl>(text) {
    override fun getContentRanges(): List<TextRange> {
        val firstContentNode = myHost.getFirstContentNode()
            ?: return emptyList()

        val result: MutableList<TextRange> = ArrayList()
        val contentRange = TextRange.create(firstContentNode.startOffset, myHost.textRange.endOffset)
            .shiftRight(-myHost.textRange.startOffset)

        val lines = StringUtil.split(contentRange.substring(myHost.text), "\n", true, false)
        // First line has opening quote
        var cumulativeOffset = contentRange.startOffset
        for (i in lines.indices) {
            val line = lines[i]

            var lineStart = 0
            var lineEnd = line.length
            if (i == 0) {
                lineStart++
            } else {
                while (lineStart < line.length && DigdagGrammarCharUtil.isSpaceLike(line[lineStart])) {
                    lineStart++
                }
            }
            if (i == lines.size - 1) {
                // Last line has closing quote
                lineEnd--
            } else {
                while (lineEnd > lineStart && DigdagGrammarCharUtil.isSpaceLike(line[lineEnd - 1])) {
                    lineEnd--
                }
            }

            result.add(TextRange.create(lineStart, lineEnd).shiftRight(cumulativeOffset))
            cumulativeOffset += line.length + 1
        }

        return result
    }

    override fun getRangesJoiner(text: CharSequence, contentRanges: List<TextRange>, indexBefore: Int): String {
        val leftRange = contentRanges[indexBefore]
        return if (leftRange.isEmpty || !myHost.isSingleQuote() && text[leftRange.endOffset - 1] == '\\') {
            "\n"
        } else if (contentRanges[indexBefore + 1].isEmpty) {
            ""
        } else {
            " "
        }
    }
}