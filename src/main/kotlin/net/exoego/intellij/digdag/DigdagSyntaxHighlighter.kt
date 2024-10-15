package net.exoego.intellij.digdag

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import net.exoego.intellij.digdag.lexer._DigdagLexer

// https://www.jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support/syntax_highlighting_and_error_highlighting.html
class DigdagSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) =
        DigdagSyntaxHighlighter()
}


class DigdagSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        private val ATTRIBUTES: Map<IElementType, TextAttributesKey> =
            hashMapOf<IElementType, TextAttributesKey>().apply {
                put(DigdagTokenTypes.SCALAR_KEY, DigdagHighlighter.SCALAR_KEY)
                put(DigdagTokenTypes.SCALAR_STRING, DigdagHighlighter.SCALAR_STRING)
                put(DigdagTokenTypes.SCALAR_DSTRING, DigdagHighlighter.SCALAR_DSTRING)
                put(DigdagTokenTypes.SCALAR_TEXT, DigdagHighlighter.SCALAR_TEXT)
                put(DigdagTokenTypes.SCALAR_LIST, DigdagHighlighter.SCALAR_LIST)
                put(DigdagTokenTypes.COMMENT, DigdagHighlighter.COMMENT)
                put(DigdagTokenTypes.TEXT, DigdagHighlighter.TEXT)
                put(DigdagTokenTypes.LBRACE, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.RBRACE, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.LBRACKET, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.RBRACKET, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.COMMA, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.QUESTION, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.COLON, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.AMPERSAND, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.DOCUMENT_MARKER, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.SEQUENCE_MARKER, DigdagHighlighter.SIGN)
                put(DigdagTokenTypes.ANCHOR, DigdagHighlighter.ANCHOR)
                put(DigdagTokenTypes.ALIAS, DigdagHighlighter.ANCHOR)
            }
    }


    override fun getHighlightingLexer(): Lexer = FlexAdapter(_DigdagLexer())

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(ATTRIBUTES[tokenType])
}