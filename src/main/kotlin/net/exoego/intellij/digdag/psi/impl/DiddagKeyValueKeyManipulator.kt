package net.exoego.intellij.digdag.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.AbstractElementManipulator
import net.exoego.intellij.digdag.DigdagElementGenerator
import net.exoego.intellij.digdag.DigdagUtil
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagMapping
import net.exoego.intellij.digdag.psi.DigdagValue

class DiddagKeyValueKeyManipulator: AbstractElementManipulator<DigdagKeyValue>() {
    override fun handleContentChange(element: DigdagKeyValue, range: TextRange, newContent: String?): DigdagKeyValue {
        val originalContent = element.getRawKeyText() ?: return element
        if (newContent == null) return element
        val updatedKey = originalContent.replaceRange(range.startOffset, range.endOffset, newContent)

        val generator = DigdagElementGenerator.getInstance(element.project)
        val valueText = when (val value = element.getValue()) {
            is DigdagMapping -> "${preserveIndent(value)}${value.text}"
            else -> element.getValueText()
        }
        return generator.createDigdagKeyValue(updatedKey, valueText).also { element.replace(it) }
    }

    private fun preserveIndent(value: DigdagValue): String {
        val indent = DigdagUtil.getIndentInThisLine(value).takeIf { it > 0 } ?: return ""
        return StringUtil.repeat(" ", indent)
    }

    /**
     * @return range of unquoted key text
     */
    override fun getRangeInElement(element: DigdagKeyValue): TextRange {
        // don't use YAMLKeyValue#keyText since it implicitly unquotes text making it impossible to calculate right range
        val content = element.getRawKeyText() ?: return TextRange.EMPTY_RANGE
        val startOffset = if (content.startsWith("'") || content.startsWith("\"")) 1 else 0
        val endOffset = if (content.length > 1 && (content.endsWith("'") || content.endsWith("\""))) -1 else 0
        return TextRange(startOffset, content.length + endOffset)
    }

    private fun DigdagKeyValue.getRawKeyText(): String? = this.getKey()?.text
}