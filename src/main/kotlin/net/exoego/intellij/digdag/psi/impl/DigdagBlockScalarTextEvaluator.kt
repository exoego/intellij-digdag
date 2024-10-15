package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.util.containers.ContainerUtil
import kotlin.math.min
import net.exoego.intellij.digdag.DigdagElementTypes
import net.exoego.intellij.digdag.DigdagTokenTypes
import org.jetbrains.annotations.Contract

abstract class DigdagBlockScalarTextEvaluator<T : DigdagBlockScalarImpl>(host: T) : DigdagScalarTextEvaluator<T>(host) {
    protected open fun shouldIncludeEolInRange(child: ASTNode): Boolean {
        if (isEol(child) && child.treeNext == null && getChompingIndicator() == ChompingIndicator.KEEP) {
            return true
        }
        return false
    }

    protected fun isEnding(rangeInHost: TextRange?): Boolean {
        if (rangeInHost == null) return true
        val lastItem = ContainerUtil.getLastItem(getContentRanges())
            ?: return false
        return rangeInHost.endOffset == lastItem.endOffset
    }

    override fun getContentRanges(): List<TextRange> {
        val firstContentChild = myHost.getFirstContentNode()
            ?: return emptyList()

        val myStart = myHost.textRange.startOffset
        val result: MutableList<TextRange> = ArrayList()

        val indent = myHost.locateIndent()

        val firstEol: ASTNode =
            TreeUtil.findSibling(firstContentChild, DigdagElementTypes.EOL_ELEMENTS)
                ?: return emptyList()

        var thisLineStart = firstEol.startOffset + 1
        var child = firstEol.treeNext
        while (child != null) {
            val childType = child.elementType
            val childRange = child.textRange

            if (childType === DigdagTokenTypes.INDENT && isEol(child.treePrev)) {
                thisLineStart = (child.startOffset + min(indent.toDouble(), child.textLength.toDouble())).toInt()
            } else if (childType === DigdagTokenTypes.SCALAR_EOL) {
                if (thisLineStart != -1) {
                    val endOffset = if (shouldIncludeEolInRange(child)) child.textRange.endOffset else child.startOffset
                    result.add(TextRange.create(thisLineStart, endOffset).shiftRight(-myStart))
                }
                thisLineStart = child.startOffset + 1
            } else {
                if (isEol(child.treeNext)) {
                    if (thisLineStart == -1) {
                        Logger.getInstance(DigdagBlockScalarTextEvaluator::class.java)
                            .warn(("thisLineStart == -1: '" + myHost.text).toString() + "'", Throwable())
                        child = child.treeNext
                        continue
                    }
                    val endOffset =
                        if (shouldIncludeEolInRange(child)) child.treeNext.textRange.endOffset else childRange.endOffset
                    result.add(TextRange.create(thisLineStart, endOffset).shiftRight(-myStart))
                    thisLineStart = -1
                }
            }
            child = child.treeNext
        }
        if (thisLineStart != -1) {
            result.add(TextRange.create(thisLineStart, myHost.textRange.endOffset).shiftRight(-myStart))
        }

        val chomping = getChompingIndicator()

        if (chomping == ChompingIndicator.KEEP) {
            return result
        }

        val lastNonEmpty = ContainerUtil.lastIndexOf(
            result
        ) { range: TextRange -> range.length != 0 }

        return if (lastNonEmpty == -1) emptyList() else result.subList(0, lastNonEmpty + 1)
    }

    /**
     * See [8.1.1.2. Block Chomping Indicator](http://www.yaml.org/spec/1.2/spec.html#id2794534)
     */
    protected fun getChompingIndicator(): ChompingIndicator {
        val headerNode = checkNotNull(myHost.getNthContentTypeChild(0))
        val header = headerNode.text

        if (header.contains("+")) {
            return ChompingIndicator.KEEP
        }
        if (header.contains("-")) {
            return ChompingIndicator.STRIP
        }

        return ChompingIndicator.CLIP
    }

    @Contract("null -> true")
    fun isEolOrNull(node: ASTNode?): Boolean {
        if (node == null) {
            return true
        }
        return DigdagElementTypes.EOL_ELEMENTS.contains(node.elementType)
    }

    /**
     * See [8.1.1.2. Block Chomping Indicator](http://www.yaml.org/spec/1.2/spec.html#id2794534)
     */
    protected enum class ChompingIndicator {
        CLIP,
        STRIP,
        KEEP
    }
}