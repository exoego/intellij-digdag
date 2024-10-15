package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ObjectUtils
import net.exoego.intellij.digdag.DigdagElementTypes
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.psi.DigdagBlockScalar
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagScalarList

class DigdagScalarListImpl(node: ASTNode) : DigdagBlockScalarImpl(node), DigdagScalarList, DigdagBlockScalar {
    override val contentType: IElementType = DigdagTokenTypes.SCALAR_LIST

    override fun getTextEvaluator(): DigdagScalarTextEvaluator<DigdagScalarListImpl> {
        return object : DigdagBlockScalarTextEvaluator<DigdagScalarListImpl>(this) {
            override fun getRangesJoiner(text: CharSequence, contentRanges: List<TextRange>, indexBefore: Int): String =
                ""

            override fun getTextValue(rangeInHost: TextRange?): String {
                var value: String = super.getTextValue(rangeInHost)
                if (!StringUtil.isEmptyOrSpaces(value)) {
                    if (getChompingIndicator() === ChompingIndicator.KEEP) {
                        if (isEnding(rangeInHost)) {
                            value += "\n"
                        }
                    }
                }
                return value
            }

            override fun shouldIncludeEolInRange(child: ASTNode): Boolean {
                if (getChompingIndicator() === ChompingIndicator.KEEP) return true

                if (isEol(child) && isEolOrNull(child.treeNext)) {
                    if (!(DigdagTokenTypes.INDENT.equals(
                            ObjectUtils.doIfNotNull(
                                child.treePrev,
                                { it.elementType })
                        ) && myHost.linesNodes.size <= 2)
                    ) {
                        return false
                    }
                }

                val next = TreeUtil.findSibling(child.treeNext, NON_SPACE_VALUES)
                return !(isEol(next) &&
                        isEolOrNull(
                            TreeUtil.findSibling(
                                next!!.treeNext,
                                NON_SPACE_VALUES
                            )
                        ) && getChompingIndicator() === ChompingIndicator.STRIP)
            }

            private val NON_SPACE_VALUES =
                TokenSet.orSet(DigdagElementTypes.SCALAR_VALUES, DigdagElementTypes.EOL_ELEMENTS)
        }
    }


    override fun updateText(text: String): PsiLanguageInjectionHost {
        val original = node.text
        val commonPrefixLength = StringUtil.commonPrefixLength(original, text)
        val commonSuffixLength = StringUtil.commonSuffixLength(original, text)
        val indent = locateIndent()

        val scalarEol: ASTNode = node.findChildByType(DigdagTokenTypes.SCALAR_EOL)
            ?: // a very strange situation
            return super.updateText(text)

        val eolOffsetInParent = scalarEol.startOffsetInParent

        val startContent = eolOffsetInParent + indent + 1
        if (startContent > commonPrefixLength) {
            // a very strange situation
            return super.updateText(text)
        }

        val originalRowPrefix = original.substring(startContent, commonPrefixLength)
        val indentString = StringUtil.repeatSymbol(' ', indent)

        val prefix = originalRowPrefix.replace(
            ("""
    
    $indentString
    """.trimIndent()).toRegex(), "\n"
        )
        val suffix = text.substring(text.length - commonSuffixLength).replace(
            ("""
    
    $indentString
    """.trimIndent()).toRegex(), "\n"
        )

        val result = prefix + text.substring(commonPrefixLength, text.length - commonSuffixLength) + suffix
        return super.updateText(result)
    }

    override fun toString(): String {
        return "Digdag scalar list"
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitScalarList(this)
        } else {
            super.accept(visitor)
        }
    }
}