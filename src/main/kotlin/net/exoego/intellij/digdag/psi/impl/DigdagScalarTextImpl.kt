package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.IElementType
import net.exoego.intellij.digdag.DigdagGrammarCharUtil
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.DigdagUtil
import net.exoego.intellij.digdag.psi.DigdagBlockScalar
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagScalarText

class DigdagScalarTextImpl(node: ASTNode) : DigdagBlockScalarImpl(node), DigdagScalarText, DigdagBlockScalar {

     override val contentType: IElementType get() = DigdagTokenTypes.SCALAR_TEXT


    override fun getTextEvaluator(): DigdagScalarTextEvaluator<DigdagScalarTextImpl> {
        return object : DigdagBlockScalarTextEvaluator<DigdagScalarTextImpl>(this) {
            override fun getRangesJoiner(
                text: CharSequence,
                contentRanges: List<TextRange>,
                indexBefore: Int
            ): String {
                val leftRange = contentRanges[indexBefore]
                val rightRange = contentRanges[indexBefore + 1]
                if (leftRange.isEmpty) {
                    if (rightRange.length == 1 && text[rightRange.startOffset] == '\n' && getChompingIndicator() !== ChompingIndicator.KEEP) return ""
                    return "\n"
                }
                if (startsWithWhitespace(text, leftRange) || startsWithWhitespace(text, rightRange)) {
                    return "\n"
                }
                if (rightRange.isEmpty) {
                    var i = indexBefore + 2
                    // Unfortunately we need to scan to the nearest non-empty line to understand
                    // whether we should add a line here
                    while (i < contentRanges.size && contentRanges[i].isEmpty) {
                        i++
                    }
                    if (i >= contentRanges.size) {
                        // empty lines until the end
                        if (getChompingIndicator() === ChompingIndicator.KEEP) {
                            return "\n"
                        }
                    } else if (startsWithWhitespace(text, contentRanges[i])) {
                        return "\n"
                    }
                    return ""
                }
                return " "
            }

            override fun getTextValue(rangeInHost: TextRange?): String {
                var value: String = super.getTextValue(rangeInHost)
                if (!StringUtil.isEmptyOrSpaces(value) && getChompingIndicator() !== ChompingIndicator.STRIP && isEnding(
                        rangeInHost
                    )
                ) {
                    value += "\n"
                }
                return value
            }

            fun startsWithWhitespace(text: CharSequence, range: TextRange): Boolean {
                if (range.isEmpty) {
                    return false
                }
                val c = text[range.startOffset]
                return c == ' ' || c == '\t'
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun getEncodeReplacements(input: CharSequence): List<Pair<TextRange, String>> {
        require(StringUtil.endsWithChar(input, '\n')) { "Should end with a line break" }

        var indent = locateIndent()
        if (indent == 0) {
            indent = DigdagUtil.getIndentToThisElement(this) + DigdagBlockScalarImpl.DEFAULT_CONTENT_INDENT
        }
        val indentString = StringUtil.repeatSymbol(' ', indent)

        val result: MutableList<Pair<TextRange, String>> = ArrayList()

        var currentLength = 0
        for (i in 0 until input.length) {
            if (input[i] == '\n') {
                result.add(
                    Pair.create(
                        TextRange.from(i, 1),
                        """
                    
                    $indentString
                    """.trimIndent()
                    )
                )
                currentLength = 0
                continue
            }

            if (currentLength > DigdagScalarImpl.MAX_SCALAR_LENGTH_PREDEFINED && input[i] == ' ' && i + 1 < input.length && DigdagGrammarCharUtil.isNonSpaceChar(
                    input[i + 1]
                )
            ) {
                result.add(
                    Pair.create(
                        TextRange.from(i, 1),
                        """
                    
                    $indentString
                    """.trimIndent()
                    )
                )
                currentLength = 0
                continue
            }

            currentLength++
        }

        return result
    }

    override fun toString(): String = "Digdag scalar text"

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitScalarText(this)
        } else {
            super.accept(visitor)
        }
    }
}