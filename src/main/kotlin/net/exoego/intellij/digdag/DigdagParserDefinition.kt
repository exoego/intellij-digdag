package net.exoego.intellij.digdag

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.exoego.intellij.digdag.lexer._DigdagLexer
import net.exoego.intellij.digdag.parser.DigdagParser
import net.exoego.intellij.digdag.psi.impl.DigdagArrayImpl
import net.exoego.intellij.digdag.psi.impl.DigdagBlockMappingImpl
import net.exoego.intellij.digdag.psi.impl.DigdagBlockSequenceImpl
import net.exoego.intellij.digdag.psi.impl.DigdagCompoundValueImpl
import net.exoego.intellij.digdag.psi.impl.DigdagDocumentImpl
import net.exoego.intellij.digdag.psi.impl.DigdagFileImpl
import net.exoego.intellij.digdag.psi.impl.DigdagHashImpl
import net.exoego.intellij.digdag.psi.impl.DigdagKeyValueImpl
import net.exoego.intellij.digdag.psi.impl.DigdagPlainTextImpl
import net.exoego.intellij.digdag.psi.impl.DigdagQuotedTextImpl
import net.exoego.intellij.digdag.psi.impl.DigdagScalarListImpl
import net.exoego.intellij.digdag.psi.impl.DigdagScalarTextImpl
import net.exoego.intellij.digdag.psi.impl.DigdagSequenceItemImpl
import net.exoego.intellij.digdag.stubs.DigdagModuleStub

class DigdagParserDefinition : ParserDefinition {
    companion object {
        val FILE = DigdagModuleStub.Type
    }

    override fun createLexer(project: Project) = FlexAdapter(_DigdagLexer())

    override fun createParser(project: Project) = DigdagParser()

    override fun createFile(viewProvider: FileViewProvider) = DigdagFileImpl(viewProvider)

    override fun getFileNodeType() = FILE

    override fun getCommentTokens() = DigdagElementTypes.YAML_COMMENT_TOKENS

    override fun getWhitespaceTokens() = DigdagElementTypes.WHITESPACE_TOKENS

    override fun getStringLiteralElements() = DigdagElementTypes.TEXT_SCALAR_ITEMS

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode) = ParserDefinition.SpaceRequirements.MAY

    override fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            DigdagElementTypes.DOCUMENT -> DigdagDocumentImpl(node)
            DigdagElementTypes.KEY_VALUE_PAIR -> DigdagKeyValueImpl(node)
            DigdagElementTypes.HASH -> DigdagHashImpl(node)
            DigdagElementTypes.ARRAY -> DigdagArrayImpl(node)
            DigdagElementTypes.SEQUENCE_ITEM -> DigdagSequenceItemImpl(node)
            DigdagElementTypes.COMPOUND_VALUE -> DigdagCompoundValueImpl(node)
            DigdagElementTypes.MAPPING -> DigdagBlockMappingImpl(node)
            DigdagElementTypes.SEQUENCE -> DigdagBlockSequenceImpl(node)
            DigdagElementTypes.SCALAR_LIST_VALUE -> DigdagScalarListImpl(node)
            DigdagElementTypes.SCALAR_TEXT_VALUE -> DigdagScalarTextImpl(node)
            DigdagElementTypes.SCALAR_PLAIN_VALUE -> DigdagPlainTextImpl(node)
            DigdagElementTypes.SCALAR_QUOTED_STRING -> DigdagQuotedTextImpl(node)
//            DigdagElementTypes.ANCHOR_NODE ->
//            DigdagElementTypes.ALIAS_NODE ->
            else -> throw IllegalArgumentException("Unknown element type: ${node.elementType}")
        }
    }
}
