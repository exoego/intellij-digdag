package net.exoego.intellij.digdag

import com.intellij.openapi.util.text.StringUtil

object DigdagGrammarCharUtil {
    private const val NS_INDICATORS: String = "-?:,\\[\\]\\{\\}#&*!|>'\\\"%@`"
    private const val NS_FLOW_INDICATORS: String = ",[]{}"
    private const val COMMON_SPACE_CHARS: String = "\n\r\t "

    fun isIndicatorChar(c: Char): Boolean = StringUtil.containsChar(NS_INDICATORS, c)

    fun isPlainSafe(c: Char): Boolean = !isSpaceLike(c) && !StringUtil.containsChar(NS_FLOW_INDICATORS, c)

    fun isSpaceLike(c: Char): Boolean = c == ' ' || c == '\t'

    fun isNonSpaceChar(c: Char): Boolean = !StringUtil.containsChar(COMMON_SPACE_CHARS, c)
}
