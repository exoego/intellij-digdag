package net.exoego.intellij.digdag

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.text.StringUtil

object DigdagTextUtil {
    fun getLineStartSafeOffset(document: Document, line: Int): Int {
        if (line >= document.lineCount) return document.textLength
        if (line < 0) return 0
        return document.getLineStartOffset(line)
    }

    fun getStartIndentSize(text: CharSequence): Int {
        var stop = 0
        while (stop < text.length) {
            val c = text[stop]
            if (!(c == ' ' || c == '\t')) {
                break
            }
            stop++
        }
        return stop
    }

    fun indentText(text: String, indent: Int): String {
        val buffer = StringBuilder()
        val indentString = StringUtil.repeatSymbol(' ', indent)
        buffer.append(indentString)
        for (c in text) {
            buffer.append(c)
            if (c == '\n') {
                buffer.append(indentString)
            }
        }
        return buffer.toString()
    }
}