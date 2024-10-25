package net.exoego.intellij.digdag

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ObjectUtils
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import java.util.*
import java.util.function.Supplier
import net.exoego.intellij.digdag.psi.DigdagDocument
import net.exoego.intellij.digdag.psi.DigdagFile
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagMapping
import net.exoego.intellij.digdag.psi.DigdagPsiElement
import net.exoego.intellij.digdag.psi.DigdagSequenceItem
import net.exoego.intellij.digdag.psi.DigdagValue
import net.exoego.intellij.digdag.psi.impl.DigdagBlockMappingImpl

object DigdagUtil {
    private val BLANK_LINE_ELEMENTS =
        TokenSet.andNot(DigdagElementTypes.BLANK_ELEMENTS, DigdagElementTypes.EOL_ELEMENTS)

    /**
     * This method return flattened key path (consist of ancestors until a document).
     *
     * YAML is frequently used in configured files.
     * Dot separator preforms access to child keys.
     * <pre>`top:
     * next:
     * list:
     * - needKey: value
    `</pre> *
     * Flattened `needKey` is `top.next.list[0].needKey`
     */
    fun getConfigFullName(target: DigdagPsiElement): String {
        return StringUtil.join(getConfigFullNameParts(target), ".")
    }

    fun getConfigFullNameParts(target: DigdagPsiElement): List<String> {
        val result: MutableList<String> = SmartList()
        var element: PsiElement? = target
        while (element != null) {
            var elementIndexSuffix = ""
            if (element is DigdagSequenceItem) {
                elementIndexSuffix = "[${element.getItemIndex()}]"
                element = PsiTreeUtil.getParentOfType(element, DigdagKeyValue::class.java)
            }
            if (element is DigdagKeyValue) {
                val keyText: String = element.getKeyText()
                result.add(keyText + elementIndexSuffix)
            }
            element = PsiTreeUtil.getParentOfType(element, DigdagKeyValue::class.java, DigdagSequenceItem::class.java)
        }
        return ContainerUtil.reverse(result)
    }

    fun getTopLevelKeys(file: DigdagFile): Collection<DigdagKeyValue> {
        val topLevelValue = file.getDocuments().get(0).getTopLevelValue()
        return if (topLevelValue is DigdagMapping) {
            topLevelValue.getKeyValues()
        } else {
            emptyList()
        }
    }

    fun getQualifiedKeyInFile(file: DigdagFile, key: List<String>): DigdagKeyValue? {
        return getQualifiedKeyInDocument(file.getDocuments().get(0), key)
    }

    fun getQualifiedKeyInDocument(document: DigdagDocument, key: List<String>): DigdagKeyValue? {
        assert(key.isNotEmpty())

        var mapping: DigdagMapping? = ObjectUtils.tryCast(document.getTopLevelValue(), DigdagMapping::class.java)
        for (i in key.indices) {
            if (mapping == null) {
                return null
            }

            var keyValue: DigdagKeyValue? = mapping.getKeyValueByKey(java.lang.String.join(".", key.subList(i, key.size)))
            if (keyValue != null) {
                return keyValue
            }

            keyValue = mapping.getKeyValueByKey(key[i])
            if (keyValue == null || i + 1 == key.size) {
                return keyValue
            }

            mapping = ObjectUtils.tryCast(keyValue.getValue(), DigdagMapping::class.java)
        }
        throw IllegalStateException("Should have returned from the loop")
    }

    fun getQualifiedKeyInFile(file: DigdagFile, vararg key: String): DigdagKeyValue? {
        return getQualifiedKeyInFile(file, Arrays.asList(*key))
    }

    fun findKeyInProbablyMapping(node: DigdagValue?, keyText: String): DigdagKeyValue? {
        if (node !is DigdagMapping) {
            return null
        }
        return node.getKeyValueByKey(keyText)
    }

    fun getValue(file: DigdagFile, vararg key: String): Pair<PsiElement, String>? {
        val record: DigdagKeyValue? = getQualifiedKeyInFile(file, *key)
        if (record != null) {
            val psiValue = record.getValue()
            return Pair.create(psiValue, record.getValueText())
        }
        return null
    }

//    fun createI18nRecord(file: DigdagFile, key: Array<String>, text: String?): DigdagKeyValue {
//        val root: DigdagDocument = checkNotNull(file.getDocuments().get(0))
//        assert(key.size > 0)
//
//        var rootMapping = PsiTreeUtil.findChildOfType(root, DigdagMapping::class.java)
//        if (rootMapping == null) {
//            val DigdagFile: DigdagFile =
//                DigdagElementGenerator.getInstance(file.getProject()).createDummyYamlWithText(key[0] + ":")
//            val mapping: DigdagMapping = checkNotNull(DigdagFile.getDocuments().get(0).getTopLevelValue() as DigdagMapping)
//            rootMapping = (root.add(mapping) as DigdagMapping)
//        }
//
//        var current: DigdagMapping? = rootMapping
//        val keyLength = key.size
//        var i = 0
//        while (i < keyLength) {
//            val existingRec = current.getKeyValueByKey(key[i])
//            if (existingRec != null) {
//                val nextMapping = ObjectUtils.tryCast(existingRec.getValue(), DigdagMapping::class.java)
//
//                if (nextMapping != null) {
//                    current = nextMapping
//                    i++
//                    continue
//                }
//            }
//
//            // Calc current key indent
//            var indent = StringUtil.repeatSymbol(' ', getIndentInThisLine(current))
//
//            // Generate items
//            val builder = StringBuilder()
//            builder.append("---")
//            for (j in i until keyLength) {
//                builder.append("\n").append(indent)
//                builder.append(key[j]).append(":")
//                indent += "  "
//            }
//            builder.append(" ").append(text)
//
//            // Create dummy mapping
//            val fileWithKey: DigdagFile =
//                DigdagElementGenerator.getInstance(file.getProject()).createDummyYamlWithText(builder.toString())
//            val dummyMapping = PsiTreeUtil.findChildOfType(
//                fileWithKey.getDocuments().get(0),
//                DigdagMapping::class.java
//            )
//            if (dummyMapping == null || dummyMapping.getKeyValues().size != 1) {
//                throw IllegalStateException("Failed to create dummy mapping")
//            }
//
//            // Add or replace
//            val dummyKeyValue: DigdagKeyValue = dummyMapping.getKeyValues().iterator().next()
//            current.putKeyValue(dummyKeyValue)
//
//            if (dummyKeyValue.getValue() !is DigdagMapping) {
//                return dummyKeyValue
//            } else {
//                current = (dummyKeyValue.getValue() as DigdagMapping)
//            }
//
//            i++
//        }
//
//        // Conflict with existing value
//        val builder = StringBuilder()
//        val top = min((i + 1).toDouble(), keyLength.toDouble()).toInt()
//        for (j in 0 until top) {
//            if (!builder.isEmpty()) {
//                builder.append('.')
//            }
//            builder.append(key[j])
//        }
//        throw IncorrectOperationException(YAMLBundle.message("new.name.conflicts.with", builder.toString()))
//    }

    fun rename(element: DigdagKeyValue, newName: String): PsiElement {
        if (newName == element.name) {
            throw IncorrectOperationException(DigdagBundle.message("rename.same.name"))
        }
        val topKeyValue: DigdagKeyValue =
            DigdagElementGenerator.getInstance(element.getProject()).createDigdagKeyValue(newName, "Foo")

        val key = element.getKey()
        check(!(key == null || topKeyValue.getKey() == null))
        key.replace(topKeyValue.getKey()!!)
        return element
    }

    fun getIndentInThisLine(elementInLine: PsiElement): Int {
        var currentElement: PsiElement? = elementInLine
        while (currentElement != null) {
            val type = currentElement.node.elementType
            if (type === DigdagTokenTypes.EOL) {
                return 0
            }
            if (type === DigdagTokenTypes.INDENT) {
                return currentElement.textLength
            }

            currentElement = PsiTreeUtil.prevLeaf(currentElement)
        }
        return 0
    }

    fun getIndentToThisElement(element: PsiElement): Int {
        var element = element
        if (element is DigdagBlockMappingImpl) {
            try {
                element = element.getFirstKeyValue()
            } catch (e: IllegalStateException) {
                // Spring Boot plug-in modifies PSI-tree into invalid state
                // This is a workaround over EA-133507 IDEA-210113
                if (e.message != DigdagBlockMappingImpl.EMPTY_MAP_MESSAGE) {
                    throw e
                } else {
                    Logger.getInstance(DigdagUtil::class.java)
                        .error(DigdagBlockMappingImpl.EMPTY_MAP_MESSAGE)
                }
            }
        }
        val offset = element.textOffset

        var currentElement: PsiElement? = element
        while (currentElement != null) {
            val type = currentElement.node.elementType
            if (DigdagElementTypes.EOL_ELEMENTS.contains(type)) {
                return offset - currentElement.textOffset - currentElement.textLength
            }

            currentElement = PsiTreeUtil.prevLeaf(currentElement)
        }
        return offset
    }

    fun psiAreAtTheSameLine(psi1: PsiElement, psi2: PsiElement): Boolean {
        var leaf = firstLeaf(psi1)
        val lastLeaf = firstLeaf(psi2)
        while (leaf != null) {
            if (PsiUtilCore.getElementType(leaf) === DigdagTokenTypes.EOL) {
                return false
            }
            if (leaf === lastLeaf) {
                return true
            }
            leaf = PsiTreeUtil.nextLeaf(leaf)
        }
        // It is a kind of magic, normally we should return from the `while` above
        return false
    }

    private fun firstLeaf(psi1: PsiElement): PsiElement? {
        val leaf = TreeUtil.findFirstLeaf(psi1.node)
        return leaf?.psi
    }

    fun deleteSurroundingWhitespace(element: PsiElement) {
        if (element.nextSibling != null) {
            deleteElementsOfType(element::getNextSibling, BLANK_LINE_ELEMENTS)
            deleteElementsOfType(element::getNextSibling, DigdagElementTypes.SPACE_ELEMENTS)
        } else {
            deleteElementsOfType(element::getPrevSibling, DigdagElementTypes.SPACE_ELEMENTS)
        }
    }

    private fun deleteElementsOfType(element: Supplier<out PsiElement?>, types: TokenSet) {
        while (element.get() != null && types.contains(PsiUtilCore.getElementType(element.get()))) {
            element.get()!!.delete()
        }
    }
}