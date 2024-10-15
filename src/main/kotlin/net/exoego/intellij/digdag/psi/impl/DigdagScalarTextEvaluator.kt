package net.exoego.intellij.digdag.psi.impl

import com.intellij.openapi.util.TextRange

abstract class DigdagScalarTextEvaluator<T : DigdagScalarImpl>(val myHost: T) {
    abstract fun getContentRanges(): List<TextRange>

    protected abstract fun getRangesJoiner(text: CharSequence, contentRanges: List<TextRange>, indexBefore: Int): String

    open fun getTextValue(rangeInHost: TextRange?): String {
        val text = myHost.text
        val contentRanges = getContentRanges()

        val builder = StringBuilder()

        for (i in contentRanges.indices) {
            val range = if (rangeInHost != null) rangeInHost.intersection(contentRanges[i]) else contentRanges[i]
            if (range == null) continue

            val curString = range.subSequence(text)
            builder.append(curString)

            if (range.endOffset == contentRanges[i].endOffset && i + 1 != contentRanges.size) {
                builder.append(getRangesJoiner(text, contentRanges, i))
            }
        }
        return DigdagScalarImpl.processReplacements(builder, myHost.getDecodeReplacements(builder))
    }
}
