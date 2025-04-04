package net.exoego.intellij.digdag.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import net.exoego.intellij.digdag.psi.DigdagMapping
import net.exoego.intellij.digdag.psi.DigdagQuotedText
import net.exoego.intellij.digdag.psi.DigdagScalarText
import net.exoego.intellij.digdag.psi.impl.DigdagMappingImpl

val HTTP_LAST_PATTERN = Regex("""\$\{\s*http\.last_content[^}]*}""")

val TD_LAST_PATTERN = Regex("""\$\{\s*td\.last_results[^}]*}""")

class DigdagExportLastValueAnnotator : BaseAnnotator() {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is DigdagMappingImpl) return

        val export = element.getKeyValueByKey("_export")?.getValue() ?: return
        if (export !is DigdagMappingImpl) {
            return
        }

        val http = mutableListOf<String>()
        val td = mutableListOf<String>()
        export.getKeyValues().forEach {
            val value = it.getValue()?.text ?: ""
            if (HTTP_LAST_PATTERN.containsMatchIn(value)) {
                http.add(it.getKeyText())
            }
            if (TD_LAST_PATTERN.containsMatchIn(value)) {
                td.add(it.getKeyText())
            }
        }
        val foundKeys = FoundKeys(http, td)
        if (foundKeys.isEmpty()) return

        findRecursively(element, foundKeys, OperatorFound(false, false), holder)
    }

    private fun findOperatorRecursively(element: DigdagMapping): OperatorFound {
        var tdFound = false
        var httpFound = false
        element.getKeyValues().forEach {
            val keyText = it.getKeyText()
            if (keyText == "td>") {
                tdFound = true
            } else if (keyText == "http>") {
                httpFound = true
            }
            val value = it.getValue() ?: return@forEach
            if (value is DigdagMapping) {
                val s = findOperatorRecursively(value)
                tdFound = tdFound || s.td
                httpFound = httpFound || s.http
            }
        }
        return OperatorFound(tdFound, httpFound)
    }

    private fun findRecursively(
        element: DigdagMapping,
        foundKeys: FoundKeys,
        operatorFound: OperatorFound,
        holder: AnnotationHolder
    ) {
        var op: OperatorFound = operatorFound

        element.getKeyValues().forEach {
            val value = it.getValue() ?: return@forEach
            if (value is DigdagMapping) {
                if (op.noFound()) {
                    op = op.merge(findOperatorRecursively(value))
                }
                findRecursively(value, foundKeys, op, holder)
                return@forEach
            }

            if (operatorFound.anyFound()) {
                if (value !is DigdagQuotedText && value !is DigdagScalarText) return@forEach

                val text = value.text

                val httpVariable = foundKeys.findHttpVariable(text)
                if (httpVariable != null) {
                    holder
                        .newAnnotation(HighlightSeverity.WARNING, "${httpVariable} is overwritten by http> after ${httpVariable} is defined")
                        .range(value)
                        .highlightType(ProblemHighlightType.GENERIC_ERROR)
                        .create()
                }

                val tdVariable = foundKeys.findTdVariable(text)
                if (tdVariable != null) {
                    holder
                        .newAnnotation(HighlightSeverity.WARNING, "${tdVariable} is overwritten by td> after ${tdVariable} is defined")
                        .range(value)
                        .highlightType(ProblemHighlightType.GENERIC_ERROR)
                        .create()
                }
            }
        }
    }
}

private data class FoundKeys(
    val http: MutableList<String>,
    val td: MutableList<String>,
) {
    fun isEmpty(): Boolean = http.isEmpty() && td.isEmpty()
    fun findHttpVariable(text: String): String? = httpPattern?.find(text)?.value
    fun findTdVariable(text: String): String? = tdPattern?.find(text)?.value

    private val httpPattern: Regex? = {
        if (http.isEmpty()) {
            null
        } else {
            val keyNamePattern = http.joinToString("|", prefix = "(?:", postfix = ")")
            val pattern = """\$\{\s*${keyNamePattern}[^}]*}"""
            Regex(pattern)
        }
    }()

    private val tdPattern: Regex? = {
        if (td.isEmpty()) {
            null
        } else {
            val keyNamePattern = td.joinToString("|", prefix = "(?:", postfix = ")")
            val pattern = """\$\{\s*${keyNamePattern}[^}]*}"""
            Regex(pattern)
        }
    }()

}

private data class OperatorFound(val td: Boolean, val http: Boolean) {
    fun anyFound(): Boolean = td || http

    fun noFound(): Boolean = !td && !http

    fun merge(other: OperatorFound): OperatorFound = OperatorFound(td || other.td, http || other.http)
}
