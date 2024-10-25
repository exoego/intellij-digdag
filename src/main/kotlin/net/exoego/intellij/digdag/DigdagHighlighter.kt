package net.exoego.intellij.digdag

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object DigdagHighlighter {
    const val SCALAR_KEY_ID: String = "DIGDAG_SCALAR_KEY"
    const val SCALAR_TEXT_ID: String = "DIGDAG_SCALAR_VALUE"
    const val SCALAR_STRING_ID: String = "DIGDAG_SCALAR_STRING"
    const val SCALAR_DSTRING_ID: String = "DIGDAG_SCALAR_DSTRING"
    const val SCALAR_LIST_ID: String = "DIGDAG_SCALAR_LIST"
    const val COMMENT_ID: String = "DIGDAG_COMMENT"
    const val TEXT_ID: String = "DIGDAG_TEXT"
    const val SIGN_ID: String = "DIGDAG_SIGN"
    const val ANCHOR_ID: String = "DIGDAG_ANCHOR"

    // text attributes keys
    val SCALAR_KEY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(SCALAR_KEY_ID, DefaultLanguageHighlighterColors.KEYWORD)
    val SCALAR_TEXT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(SCALAR_TEXT_ID, HighlighterColors.TEXT)
    val SCALAR_STRING: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(SCALAR_STRING_ID, DefaultLanguageHighlighterColors.STRING)
    val SCALAR_DSTRING: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(SCALAR_DSTRING_ID, DefaultLanguageHighlighterColors.STRING)
    val SCALAR_LIST: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(SCALAR_LIST_ID, HighlighterColors.TEXT)
    val COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(COMMENT_ID, DefaultLanguageHighlighterColors.DOC_COMMENT)
    val TEXT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(TEXT_ID, HighlighterColors.TEXT)
    val SIGN: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(SIGN_ID, DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val ANCHOR: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(ANCHOR_ID, DefaultLanguageHighlighterColors.IDENTIFIER)
}