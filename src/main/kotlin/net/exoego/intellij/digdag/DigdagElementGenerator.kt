package net.exoego.intellij.digdag

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter
import java.util.stream.Collectors
import net.exoego.intellij.digdag.psi.DigdagFile
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagScalar
import net.exoego.intellij.digdag.psi.DigdagSequence
import net.exoego.intellij.digdag.psi.DigdagSequenceItem
import net.exoego.intellij.digdag.psi.DigdagValue
import net.exoego.intellij.digdag.psi.impl.DigdagQuotedTextImpl

class DigdagElementGenerator(private val myProject: Project) {
    companion object {
        fun getInstance(project: Project): DigdagElementGenerator =
            project.getService(DigdagElementGenerator::class.java)

        fun createChainedKey(keyComponents: List<String?>, indentAddition: Int): String {
            val sb = StringBuilder()
            for (i in keyComponents.indices) {
                if (i > 0) {
                    sb.append(StringUtil.repeatSymbol(' ', indentAddition + 2 * i))
                }
                sb.append(keyComponents[i]).append(":")
                if (i + 1 < keyComponents.size) {
                    sb.append('\n')
                }
            }
            return sb.toString()
        }
    }

    fun createDigdagKeyValueWithSequence(keyName: String, elementsMap: Map<String, String>): DigdagKeyValue {
        val digdagString = elementsMap
            .entries.stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .map { entry: Map.Entry<String?, String?> -> String.format("%s: %s", entry.key, entry.value) }
            .collect(Collectors.joining("\n"))
        return createDigdagKeyValue(keyName, digdagString)
    }

    fun createDigdagKeyValue(keyName: String, valueText: String): DigdagKeyValue {
        val tempValueFile: PsiFile = createDummyDigdagWithText(valueText)
        val values: Collection<DigdagValue> = PsiTreeUtil.collectElementsOfType(
            tempValueFile,
            DigdagValue::class.java
        )
        val text = if (values.isEmpty()) {
            "$keyName:"
        } else if (values.iterator().next() is DigdagScalar && !valueText.contains("\n")) {
            "$keyName: $valueText"
        } else {
            """
        $keyName:
        ${DigdagTextUtil.indentText(valueText, 2)}
        """.trimIndent()
        }

        val tempFile: PsiFile = createDummyDigdagWithText(text)
        return PsiTreeUtil.collectElementsOfType(tempFile, DigdagKeyValue::class.java).iterator().next()
    }

    fun createDigdagDoubleQuotedString(): DigdagQuotedTextImpl {
        val tempFile: DigdagFile = createDummyDigdagWithText("\"foo\"")
        return PsiTreeUtil.collectElementsOfType(tempFile, DigdagQuotedTextImpl::class.java).iterator().next()
    }

    fun createDummyDigdagWithText(text: String): DigdagFile {
        return PsiFileFactory.getInstance(myProject)
            .createFileFromText(
                "temp." + DigdagFileType.getDefaultExtension(),
                DigdagFileType,
                text,
                LocalTimeCounter.currentTime(),
                false
            ) as DigdagFile
    }

    fun createEol(): PsiElement {
        val file: DigdagFile = createDummyDigdagWithText("\n")
        return PsiTreeUtil.getDeepestFirst(file)
    }

    fun createSpace(): PsiElement {
        val keyValue: DigdagKeyValue = createDigdagKeyValue("foo", "bar")
        val whitespaceNode: ASTNode = checkNotNull(keyValue.getNode().findChildByType(TokenType.WHITE_SPACE))
        return whitespaceNode.psi
    }

    fun createIndent(size: Int): PsiElement {
        val file: DigdagFile = createDummyDigdagWithText(StringUtil.repeatSymbol(' ', size))
        return PsiTreeUtil.getDeepestFirst(file)
    }

    fun createColon(): PsiElement {
        val file: DigdagFile = createDummyDigdagWithText("? foo : bar")
        val at = file.findElementAt("? foo ".length)
        assert(at != null && at.node.elementType === DigdagTokenTypes.COLON)
        return at!!
    }

    fun createComma(): PsiElement {
        val file: DigdagFile = createDummyDigdagWithText("[1,2]")
        val comma = file.findElementAt("[1".length)
        assert(comma != null && comma.node.elementType === DigdagTokenTypes.COMMA)
        return comma!!
    }

    fun createDocumentMarker(): PsiElement {
        val file: DigdagFile = createDummyDigdagWithText("---")
        val at = file.findElementAt(0)
        assert(at != null && at.node.elementType === DigdagTokenTypes.DOCUMENT_MARKER)
        return at!!
    }

    fun createEmptySequence(): DigdagSequence {
        val sequence: DigdagSequence = checkNotNull(
            PsiTreeUtil.findChildOfType(
                createDummyDigdagWithText("- dummy"),
                DigdagSequence::class.java
            )
        )
        sequence.deleteChildRange(sequence.getFirstChild(), sequence.getLastChild())
        return sequence
    }

    fun createEmptyArray(): DigdagSequence {
        val sequence: DigdagSequence = checkNotNull(
            PsiTreeUtil.findChildOfType(
                createDummyDigdagWithText("[]"),
                DigdagSequence::class.java
            )
        )
        return sequence
    }

    fun createEmptySequenceItem(): DigdagSequenceItem {
        val sequenceItem: DigdagSequenceItem = checkNotNull(
            PsiTreeUtil.findChildOfType(
                createDummyDigdagWithText("- dummy"),
                DigdagSequenceItem::class.java
            )
        )
        val value: DigdagValue = checkNotNull(sequenceItem.getValue())
        value.deleteChildRange(value.getFirstChild(), value.getLastChild())
        return sequenceItem
    }

    fun createSequenceItem(text: String): DigdagSequenceItem {
        val sequenceItem: DigdagSequenceItem = checkNotNull(
            PsiTreeUtil.findChildOfType(
                createDummyDigdagWithText(
                    "- $text"
                ),
                DigdagSequenceItem::class.java
            )
        )
        val value: DigdagValue = checkNotNull(sequenceItem.getValue())
        return sequenceItem
    }

    fun createArrayItem(text: String): DigdagSequenceItem {
        val sequenceItem: DigdagSequenceItem = checkNotNull(
            PsiTreeUtil.findChildOfType(
                createDummyDigdagWithText(
                    "[$text]"
                ),
                DigdagSequenceItem::class.java
            )
        )
        val value: DigdagValue = checkNotNull(sequenceItem.getValue())
        return sequenceItem
    }
}