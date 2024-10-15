package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.psi.DigdagScalar

class DigdagPlainTextImpl(node: ASTNode) : DigdagBlockScalarImpl(node), DigdagScalar {
    override val contentType: IElementType get() = DigdagTokenTypes.TEXT

    override val includeFirstLineInContent: Boolean get() = true

    override fun getTextEvaluator(): DigdagScalarTextEvaluator<DigdagPlainTextImpl> {
        return object : DigdagScalarTextEvaluator<DigdagPlainTextImpl>(this) {
            override fun getContentRanges(): List<TextRange> {
                val myStart = textRange.startOffset
                val result: MutableList<TextRange> = ArrayList()

                var seenText = false
                var child = getFirstContentNode()
                while (child != null) {
                    if (child.elementType === DigdagTokenTypes.TEXT) {
                        seenText = true
                        result.add(child.textRange.shiftRight(-myStart))
                    } else if (child.elementType === DigdagTokenTypes.EOL) {
                        if (!seenText) {
                            result.add(child.textRange.shiftRight(-myStart))
                        }
                        seenText = false
                    }
                    child = child.treeNext
                }

                return result
            }

            override fun getRangesJoiner(
                text: CharSequence,
                contentRanges: List<TextRange>,
                indexBefore: Int
            ): String {
                return if (isNewline(text, contentRanges[indexBefore]) ||
                    isNewline(text, contentRanges[indexBefore + 1])
                ) {
                    ""
                } else {
                    " "
                }
            }

            fun isNewline(text: CharSequence, range: TextRange): Boolean {
                return range.length == 1 && text[range.startOffset] == '\n'
            }
        }
    }

    override fun toString(): String {
        return "Digdag plain scalar text"
    }

    override fun isMultiline(): Boolean {
        return node.findChildByType(DigdagTokenTypes.EOL) != null
    }
}