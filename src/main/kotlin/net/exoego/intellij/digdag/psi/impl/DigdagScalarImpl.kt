package net.exoego.intellij.digdag.psi.impl

import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementManipulators
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import javax.swing.Icon
import net.exoego.intellij.digdag.DigdagElementTypes
import net.exoego.intellij.digdag.DigdagGrammarCharUtil
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagScalar
import net.exoego.intellij.digdag.psi.impl.DigdagScalarImpl.Companion.processReplacements

abstract class DigdagScalarImpl(node: ASTNode) : DigdagValueImpl(node), DigdagScalar{
    companion object {
        const val MAX_SCALAR_LENGTH_PREDEFINED: Int = 60

        @Throws(IndexOutOfBoundsException::class)
        fun processReplacements(
            input: CharSequence,
            replacements: List<Pair<TextRange, String>>
        ): String {
            val result = StringBuilder()
            var currentOffset = 0
            for (replacement in replacements) {
                result.append(input.subSequence(currentOffset, replacement.getFirst().startOffset))
                result.append(replacement.getSecond())
                currentOffset = replacement.getFirst().endOffset
            }
            result.append(input.subSequence(currentOffset, input.length))
            return result.toString()
        }
    }

    abstract fun getContentRanges(): List<TextRange>

    abstract fun getTextEvaluator(): DigdagScalarTextEvaluator<*>

    open fun getDecodeReplacements(input: CharSequence): List<Pair<TextRange, String>> = emptyList()

    @Throws(IllegalArgumentException::class)
    open  fun getEncodeReplacements(input: CharSequence): List<Pair<TextRange, String>> = emptyList()

    override fun getTextValue(): String = getTextEvaluator().getTextValue(null)

    fun getTextValue(rangeInHost: TextRange?): String = getTextEvaluator().getTextValue(rangeInHost)

    override fun getReference(): PsiReference? {
        val references = references
        return if (references.size == 1) references[0] else null
    }

    override fun getReferences(): Array<PsiReference> = ReferenceProvidersRegistry.getReferencesFromProviders(this)

    override fun isValidHost(): Boolean = true

    override fun updateText(text: String): PsiLanguageInjectionHost =
        ElementManipulators.handleContentChange(this, text)

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return MyLiteralTextEscaper(this)
    }

    protected fun isSurroundedByNoSpace(text: CharSequence, pos: Int): Boolean {
        return (pos - 1 < 0 || !DigdagGrammarCharUtil.isSpaceLike(text[pos - 1]))
                && (pos + 1 >= text.length || !DigdagGrammarCharUtil.isSpaceLike(text[pos + 1]))
    }

    fun getFirstContentNode(): ASTNode? {
        var node = node.firstChildNode
        while (node != null && (/*node.elementType === DigdagTokenTypes.TAG || */DigdagElementTypes.BLANK_ELEMENTS.contains(node.elementType))) {
            node = node.treeNext
        }
        return node
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitScalar(this)
        } else {
            super.accept(visitor)
        }
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String {
                return StringUtil.shortenTextWithEllipsis(getTextValue(), 20, 0, true)
            }

            override fun getLocationString(): String {
                return containingFile.name
            }

            override fun getIcon(unused: Boolean): Icon {
                return AllIcons.Nodes.Variable
            }
        }
    }
}

private class MyLiteralTextEscaper(scalar: DigdagScalarImpl) : LiteralTextEscaper<DigdagScalarImpl>(scalar) {
    private var text: String? = null
    private var contentRanges: List<TextRange>? = null

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        text = myHost.getText()
        contentRanges = myHost.getContentRanges()
        var decoded = false
        for (range in contentRanges!!) {
            val intersection = range.intersection(rangeInsideHost) ?: continue
            decoded = true
            val substring = intersection.substring(text!!)
            outChars.append(processReplacements(substring, myHost.getDecodeReplacements(substring)))
        }
        return decoded
    }

    override fun getRelevantTextRange(): TextRange {
        if (contentRanges == null) {
            contentRanges = myHost.getContentRanges()
        }
        if (contentRanges!!.isEmpty()) return TextRange.EMPTY_RANGE
        return TextRange.create(
            contentRanges!![0].startOffset, contentRanges!![contentRanges!!.size - 1].endOffset
        )
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        var currentOffsetInDecoded = 0

        var last: TextRange? = null
        for (i in contentRanges!!.indices) {
            val range = rangeInsideHost.intersection(contentRanges!![i]) ?: continue
            last = range

            val curString = range.subSequence(text!!).toString()

            val replacementsForThisLine = myHost.getDecodeReplacements(curString)
            var encodedOffsetInCurrentLine = 0
            for (replacement in replacementsForThisLine) {
                val deltaLength = replacement.getFirst().startOffset - encodedOffsetInCurrentLine
                val currentOffsetBeforeReplacement = currentOffsetInDecoded + deltaLength
                if (currentOffsetBeforeReplacement > offsetInDecoded) {
                    return range.startOffset + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
                } else if (currentOffsetBeforeReplacement == offsetInDecoded && replacement.getSecond().isNotEmpty()) {
                    return range.startOffset + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
                }
                currentOffsetInDecoded += deltaLength + replacement.getSecond().length
                encodedOffsetInCurrentLine += deltaLength + replacement.getFirst().length
            }

            val deltaLength = curString.length - encodedOffsetInCurrentLine
            if (currentOffsetInDecoded + deltaLength > offsetInDecoded) {
                return range.startOffset + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded)
            }
            currentOffsetInDecoded += deltaLength
        }

        return last?.endOffset ?: -1
    }

    override fun isOneLine(): Boolean {
        return !myHost.isMultiline()
    }
}