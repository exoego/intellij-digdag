package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import net.exoego.intellij.digdag.DigdagElementGenerator
import net.exoego.intellij.digdag.DigdagElementTypes
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.DigdagUtil
import net.exoego.intellij.digdag.psi.DigdagKeyValue

class DigdagBlockMappingImpl(node: ASTNode) : DigdagMappingImpl(node){
    companion object {
        const val EMPTY_MAP_MESSAGE: String = "Digdag map without any key-value"
    }

    fun getFirstKeyValue(): DigdagKeyValue {
        val firstKeyValue: DigdagKeyValue? = findChildByType(DigdagElementTypes.KEY_VALUE_PAIR)
        checkNotNull(firstKeyValue) { EMPTY_MAP_MESSAGE }
        return firstKeyValue
    }

    private fun addNewKeyToTheEnd(key: DigdagKeyValue) {
        val indent: Int = DigdagUtil.getIndentToThisElement(this)

        val generator: DigdagElementGenerator = DigdagElementGenerator.getInstance(project)
        val lastChildType = PsiUtilCore.getElementType(lastChild)
        if (indent == 0) {
            if (lastChildType !== DigdagTokenTypes.EOL) {
                add(generator.createEol())
            }
        } else if (!(lastChildType === DigdagTokenTypes.INDENT && lastChild.textLength === indent)) {
            add(generator.createEol())
            add(generator.createIndent(indent))
        }
        add(key)
    }

    override fun addNewKey(key: DigdagKeyValue) {
        val indent: Int = DigdagUtil.getIndentToThisElement(this)
        val node = node
        var place = node.lastChildNode
        var whereInsert: ASTNode? = null
        while (place != null) {
            if (place.elementType === DigdagTokenTypes.INDENT && place.textLength == indent) {
                whereInsert = place
            } else if (place.elementType === DigdagTokenTypes.EOL) {
                val next = place.treeNext
                if (next == null || next.elementType === DigdagTokenTypes.EOL) {
                    whereInsert = place
                }
            } else {
                break
            }
            place = place.treePrev
        }

        val generator: DigdagElementGenerator = DigdagElementGenerator.getInstance(project)
        if (whereInsert == null) {
            add(generator.createEol())
            if (indent != 0) {
                add(generator.createIndent(indent))
            }
            add(key)
            return
        }

        var anchor = whereInsert.psi
        if (indent == 0 || whereInsert.elementType === DigdagTokenTypes.INDENT && lastChild.textLength === indent) {
            addAfter(key, anchor)
            return
        }
        if (whereInsert.elementType !== DigdagTokenTypes.EOL) {
            anchor = addAfter(generator.createEol(), anchor)
        }
        addAfter(generator.createIndent(indent), anchor)
        addAfter(key, anchor)
    }

    /**
     * This method inserts key-value pair somewhere near specified absolute offset.
     * The offset could be beyond borders of this mapping.
     */
    fun insertKeyValueAtOffset(keyValue: DigdagKeyValue, offset: Int) {
        var offset = offset
        val indent: Int = DigdagUtil.getIndentToThisElement(this)

        if (offset < textRange.startOffset) {
            offset = textRange.startOffset
        }

        val generator: DigdagElementGenerator = DigdagElementGenerator.getInstance(project)
        if (offset == textRange.startOffset) {
            val pasteAtEmptyFirstMappingLine = PsiUtilCore.getElementType(prevSibling) === DigdagTokenTypes.INDENT &&
                    PsiUtilCore.getElementType(firstChild) === DigdagTokenTypes.EOL
            var newElement = addBefore(keyValue, firstChild)
            if (!pasteAtEmptyFirstMappingLine) {
                newElement = addAfter(generator.createEol(), newElement)
                addAfter(generator.createIndent(indent), newElement)
            }
            return
        }

        if (offset == textRange.endOffset) {
            addNewKeyToTheEnd(keyValue)
            return
        }

        if (offset > textRange.endOffset) {
            var nextLeaf = PsiTreeUtil.nextLeaf(this)
            val toBeRemoved: MutableList<PsiElement?> = SmartList()
            while (DigdagElementTypes.SPACE_ELEMENTS.contains(PsiUtilCore.getElementType(nextLeaf))) {
                if (offset >= nextLeaf!!.textRange.startOffset) {
                    toBeRemoved.add(nextLeaf)
                }
                nextLeaf = PsiTreeUtil.nextLeaf(nextLeaf)
            }
            for (leaf in toBeRemoved) {
                add(leaf!!)
            }
            for (leaf in toBeRemoved) {
                leaf!!.delete()
            }

            addNewKeyToTheEnd(keyValue)
            return
        }

        var child = firstChild
        while (child != null) {
            if (PsiUtilCore.getElementType(child) === DigdagTokenTypes.INDENT && offset <= child.textRange.endOffset) {
                if (PsiUtilCore.getElementType(child.nextSibling) === DigdagTokenTypes.EOL) {
                    addAfter(keyValue, child)
                    return
                }
                var newElement = addBefore(generator.createIndent(indent), child)
                newElement = addAfter(keyValue, newElement)
                addAfter(generator.createEol(), newElement)
                return
            }
            if (offset <= child.textRange.endOffset) {
                break
            }
            child = child.nextSibling
        }
        while (child != null) {
            if (PsiUtilCore.getElementType(child) === DigdagTokenTypes.EOL) {
                var element = child
                if (indent != 0) {
                    element = addAfter(generator.createIndent(indent), element)
                }
                element = addAfter(keyValue, element)
                if (PsiUtilCore.getElementType(child) !== DigdagTokenTypes.EOL) {
                    addAfter(generator.createEol(), element)
                }
                return
            }
            child = child.nextSibling
        }
        addNewKeyToTheEnd(keyValue)
    }

    /** @return deepest created or found key or null if nothing could be created
     */
    fun getOrCreateKeySequence(keyComponents: List<String?>, preferableOffset: Int): DigdagKeyValue? {
        if (keyComponents.isEmpty()) {
            return null
        }
        val head = keyComponents.iterator().next()
        val tail = ContainerUtil.subList(keyComponents, 1)

        var keyValue: DigdagKeyValue? = getKeyValueByKey(head!!)
        if (keyValue == null) {
            val indent: Int = DigdagUtil.getIndentToThisElement(this)
            val text: String = DigdagElementGenerator.createChainedKey(keyComponents, indent)
            val generator = DigdagElementGenerator.getInstance(project)
            val values: Collection<DigdagKeyValue> = PsiTreeUtil.collectElementsOfType(
                generator.createDummyDigdagWithText(text),
                DigdagKeyValue::class.java
            )
            if (values.isEmpty()) {
                Logger.getInstance(DigdagBlockMappingImpl::class.java).error(
                    "No one key-value created: input sequence = $keyComponents generated text = '$text'"
                )
                return null
            }
            val newKeyValue: DigdagKeyValue = values.iterator().next()
            insertKeyValueAtOffset(newKeyValue, preferableOffset)
            keyValue = getKeyValueByKey(head)
            checkNotNull(keyValue)
        }

        return if (keyComponents.size == 1) {
            keyValue
        } else if (keyValue.getValue() is DigdagBlockMappingImpl) { // TODO: support JSON-like mappings (create minor issue)
            (keyValue.getValue() as DigdagBlockMappingImpl).getOrCreateKeySequence(
                tail,
                preferableOffset
            )
        } else {
            null
        }
    }
}