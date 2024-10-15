package net.exoego.intellij.digdag.parser


import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.containers.Stack
import net.exoego.intellij.digdag.DigdagBundle
import net.exoego.intellij.digdag.DigdagElementTypes
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.DigdagTokenTypes.COLON
import net.exoego.intellij.digdag.DigdagTokenTypes.COMMA
import net.exoego.intellij.digdag.DigdagTokenTypes.COMMENT
import net.exoego.intellij.digdag.DigdagTokenTypes.DOCUMENT_MARKER
import net.exoego.intellij.digdag.DigdagTokenTypes.EOL
import net.exoego.intellij.digdag.DigdagTokenTypes.INDENT
import net.exoego.intellij.digdag.DigdagTokenTypes.LBRACE
import net.exoego.intellij.digdag.DigdagTokenTypes.LBRACKET
import net.exoego.intellij.digdag.DigdagTokenTypes.QUESTION
import net.exoego.intellij.digdag.DigdagTokenTypes.RBRACE
import net.exoego.intellij.digdag.DigdagTokenTypes.RBRACKET
import net.exoego.intellij.digdag.DigdagTokenTypes.SCALAR_DSTRING
import net.exoego.intellij.digdag.DigdagTokenTypes.SCALAR_EOL
import net.exoego.intellij.digdag.DigdagTokenTypes.SCALAR_KEY
import net.exoego.intellij.digdag.DigdagTokenTypes.SCALAR_LIST
import net.exoego.intellij.digdag.DigdagTokenTypes.SCALAR_STRING
import net.exoego.intellij.digdag.DigdagTokenTypes.SCALAR_TEXT
import net.exoego.intellij.digdag.DigdagTokenTypes.SEQUENCE_MARKER
import net.exoego.intellij.digdag.DigdagTokenTypes.STAR
import net.exoego.intellij.digdag.DigdagTokenTypes.TEXT
import net.exoego.intellij.digdag.DigdagTokenTypes.WHITESPACE


class DigdagParser : PsiParser, LightPsiParser {
    companion object {
        val HASH_STOP_TOKENS: TokenSet = TokenSet.create(RBRACE, COMMA)
        val ARRAY_STOP_TOKENS: TokenSet = TokenSet.create(RBRACKET, COMMA)
    }

    private var myBuilder: PsiBuilder? = null
    private var eolSeen = false
    private var myIndent = 0
    private var myAfterLastEolMarker: PsiBuilder.Marker? = null

    private val myStopTokensStack = Stack<TokenSet>()

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        parseLight(root, builder)
        return builder.treeBuilt
    }

    override fun parseLight(root: IElementType?, builder: PsiBuilder?) {
        myBuilder = builder
        myStopTokensStack.clear()
        val fileMarker = mark()
        parseFile()
        assert(myBuilder!!.eof()) { "Not all tokens were passed." }
        fileMarker.done(root!!)
    }

    private fun parseFile() {
        val marker = mark()
        passJunk()
        if (myBuilder!!.tokenType !== DOCUMENT_MARKER) {
            dropEolMarker()
            marker.rollbackTo()
        } else {
            marker.drop()
        }
        do {
            parseDocument()
            passJunk()
        } while (!myBuilder!!.eof())
        dropEolMarker()
    }

    private fun parseDocument() {
        val marker = mark()
        if (myBuilder!!.tokenType === DOCUMENT_MARKER) {
            advanceLexer()
        }
        parseBlockNode(myIndent, false)
        dropEolMarker()
        marker.done(DigdagElementTypes.DOCUMENT)
    }

    private fun parseBlockNode(indent: Int, insideSequence: Boolean) {
        // Preserve most test and current behaviour for most general cases without comments
        if (getTokenType() === EOL) {
            advanceLexer()
            if (getTokenType() === INDENT) {
                advanceLexer()
            }
        }

        val marker = mark()
        passJunk()

        var endOfNodeMarker: PsiBuilder.Marker? = null
        var nodeType: IElementType? = null


        // It looks like tag for a block node should be located on a separate line
//        if (getTokenType() === DigdagTokenTypes.TAG && myBuilder!!.lookAhead(1) === DigdagTokenTypes.EOL) {
//            advanceLexer()
//        }

        var numberOfItems = 0
        while (!eof() && (isJunk() || !eolSeen || myIndent + getIndentBonus(insideSequence) >= indent)) {
            if (isJunk()) {
                advanceLexer()
                continue
            }

            if (!myStopTokensStack.isEmpty() && myStopTokensStack.peek().contains(getTokenType())) {
                rollBackToEol()
                break
            }

            numberOfItems++
            val parsedTokenType = parseSingleStatement(if (eolSeen) myIndent else indent, indent)
            if (nodeType == null) {
                if (parsedTokenType === DigdagElementTypes.SEQUENCE_ITEM) {
                    nodeType = DigdagElementTypes.SEQUENCE
                } else if (parsedTokenType === DigdagElementTypes.KEY_VALUE_PAIR) {
                    nodeType = DigdagElementTypes.MAPPING
                } else if (numberOfItems > 1) {
                    nodeType = DigdagElementTypes.COMPOUND_VALUE
                }
            }
            endOfNodeMarker?.drop()
            endOfNodeMarker = mark()
        }

        if (endOfNodeMarker != null) {
            dropEolMarker()
            endOfNodeMarker.rollbackTo()
        } else {
            rollBackToEol()
        }

        includeBlockEmptyTail(indent)

        if (nodeType != null) {
            marker.done(nodeType)
            marker.setCustomEdgeTokenBinders(
                { tokens: List<IElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter? ->
                    findLeftRange(
                        tokens
                    )
                },
                { tokens: List<IElementType?>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter? -> tokens.size })
        } else {
            marker.drop()
        }
    }

    private fun includeBlockEmptyTail(indent: Int) {
        if (indent == 0) {
            // top-level block with zero indent
            while (isJunk()) {
                if (getTokenType() === EOL) {
                    if (!DigdagElementTypes.BLANK_ELEMENTS.contains(myBuilder!!.lookAhead(1))) {
                        // do not include last \n into block
                        break
                    }
                }
                advanceLexer()
                dropEolMarker()
            }
        } else {
            var endOfBlock = mark()
            while (isJunk()) {
                if (getTokenType() === INDENT && getCurrentTokenLength() >= indent) {
                    dropEolMarker()
                    endOfBlock.drop()
                    advanceLexer()
                    endOfBlock = mark()
                } else {
                    advanceLexer()
                    dropEolMarker()
                }
            }
            endOfBlock.rollbackTo()
        }
    }

    /**
     * @link {http://www.Digdag.org/spec/1.2/spec.html#id2777534}
     */
    private fun getIndentBonus(insideSequence: Boolean): Int {
        return if (!insideSequence && getTokenType() === SEQUENCE_MARKER) {
            1
        } else {
            0
        }
    }

    private fun getShorthandIndentAddition(): Int {
        val offset = myBuilder!!.currentOffset
        val nextToken = myBuilder!!.lookAhead(1)
        if (nextToken !== SEQUENCE_MARKER && nextToken !== SCALAR_KEY) {
            return 1
        }
        return if (myBuilder!!.rawLookup(1) === WHITESPACE) {
            myBuilder!!.rawTokenTypeStart(2) - offset
        } else {
            1
        }
    }

    private fun parseSingleStatement(indent: Int, minIndent: Int): IElementType? {
        if (eof()) {
            return null
        }

        val marker = mark()
        parseNodeProperties()

        val tokenType = getTokenType()
        val nodeType: IElementType?
        if (tokenType === LBRACE) {
            nodeType = parseHash()
        } else if (tokenType === LBRACKET) {
            nodeType = parseArray()
        } else if (tokenType === SEQUENCE_MARKER) {
            nodeType = parseSequenceItem(indent)
        } else if (tokenType === QUESTION) {
            nodeType = parseExplicitKeyValue(indent)
        } else if (tokenType === SCALAR_KEY) {
            nodeType = parseScalarKeyValue(indent)
        } else if (DigdagElementTypes.SCALAR_VALUES.contains(getTokenType())) {
            nodeType = parseScalarValue(minIndent)
        } else if (tokenType === STAR) {
            val aliasMarker = mark()
            advanceLexer() // symbol *
            if (getTokenType() === DigdagTokenTypes.ALIAS) {
                advanceLexer() // alias name
                aliasMarker.done(DigdagElementTypes.ALIAS_NODE)
                if (getTokenType() === COLON) {
                    // Alias is used as key name
                    eolSeen = false
                    val indentAddition = getShorthandIndentAddition()
                    nodeType = parseSimpleScalarKeyValueFromColon(indent, indentAddition)
                } else {
                    // simple ALIAS_NODE was constructed and marker should be dropped
                    marker.drop()
                    return DigdagElementTypes.ALIAS_NODE
                }
            } else {
                // Should be impossible now (because of lexer rules)
                aliasMarker.drop()
                nodeType = null
            }
        } else {
            advanceLexer()
            nodeType = null
        }

        if (nodeType != null) {
            marker.done(nodeType)
        } else {
            marker.drop()
        }
        return nodeType
    }

    /**
     * Each node may have two optional properties, anchor and tag, in addition to its content.
     * Node properties may be specified in any order before the node’s content.
     * Either or both may be omitted.
     *
     * <pre>
     * [96] c-ns-properties(n,c) ::= ( c-ns-tag-property ( s-separate(n,c) c-ns-anchor-property )? )
     * | ( c-ns-anchor-property ( s-separate(n,c) c-ns-tag-property )? )
     *
    </pre> *
     * See [6.9. Node Properties](http://www.Digdag.org/spec/1.2/spec.html#id2783797)
     */
    private fun parseNodeProperties() {
        // By standard here could be no more than one TAG or ANCHOR
        // By better to support sequence of them
        var anchorWasRead = false
        var tagWasRead = false
        while (/*getTokenType() === DigdagTokenTypes.TAG ||*/ getTokenType() === DigdagTokenTypes.AMPERSAND) {
            if (getTokenType() === DigdagTokenTypes.AMPERSAND) {
                var errorMarker: PsiBuilder.Marker? = null
                if (anchorWasRead) {
                    errorMarker = mark()
                }
                anchorWasRead = true
                val anchorMarker = mark()
                advanceLexer() // symbol &
                if (getTokenType() === DigdagTokenTypes.ANCHOR) {
                    advanceLexer() // anchor name
                    anchorMarker.done(DigdagElementTypes.ANCHOR_NODE)
                } else {
                    // Should be impossible now (because of lexer rules)
                    anchorMarker.drop()
                }
                errorMarker?.error(DigdagBundle.message("DigdagParser.multiple.anchors"))
            } else { // tag case
                if (tagWasRead) {
                    val errorMarker = mark()
                    advanceLexer()
                    errorMarker.error(DigdagBundle.message("DigdagParser.multiple.tags"))
                } else {
                    tagWasRead = true
                    advanceLexer()
                }
            }
        }
    }

    private fun parseScalarValue(indent: Int): IElementType? {
        val tokenType = getTokenType()
        assert(DigdagElementTypes.SCALAR_VALUES.contains(tokenType)) { "Scalar value expected!" }
        if (tokenType === SCALAR_LIST || tokenType === SCALAR_TEXT) {
            return parseMultiLineScalar(tokenType)
        } else if (tokenType === TEXT) {
            return parseMultiLinePlainScalar(indent)
        } else if (tokenType === SCALAR_DSTRING || tokenType === SCALAR_STRING) {
            return parseQuotedString()
        } else {
            advanceLexer()
            return null
        }
    }

    private fun parseQuotedString(): IElementType {
        advanceLexer()
        return DigdagElementTypes.SCALAR_QUOTED_STRING
    }

    private fun parseMultiLineScalar(tokenType: IElementType?): IElementType {
        assert(tokenType === getTokenType())
        // Accept header token: '|' or '>'
        advanceLexer()

        // Parse header tail: TEXT is used as placeholder for invalid symbols in this context
        if (getTokenType() === TEXT) {
            val err = myBuilder!!.mark()
            advanceLexer()
            err.error(DigdagBundle.message("DigdagParser.invalid.header.symbols"))
        }

        if (DigdagElementTypes.EOL_ELEMENTS.contains(getTokenType())) {
            advanceLexer()
        }
        var endOfValue: PsiBuilder.Marker? = myBuilder!!.mark()

        var type = getTokenType()
        // Lexer ensures such input token structure: ( ( INDENT tokenType? )? SCALAR_EOL )*
        // endOfValue marker is needed to exclude INDENT after last SCALAR_EOL
        while (type === tokenType || type === INDENT || type === SCALAR_EOL) {
            advanceLexer()
            if (type === tokenType) {
                endOfValue?.drop()
                endOfValue = null
            }
            if (type === SCALAR_EOL) {
                endOfValue?.drop()
                endOfValue = myBuilder!!.mark()
            }

            type = getTokenType()
        }
        endOfValue?.rollbackTo()

        return if (tokenType === SCALAR_LIST) DigdagElementTypes.SCALAR_LIST_VALUE else DigdagElementTypes.SCALAR_TEXT_VALUE
    }

    private fun parseMultiLinePlainScalar(indent: Int): IElementType {
        var lastTextEnd: PsiBuilder.Marker? = null

        var type = getTokenType()
        while (type === TEXT || type === INDENT || type === EOL) {
            advanceLexer()

            if (type === TEXT) {
                if (lastTextEnd != null && myIndent < indent) {
                    break
                }
                lastTextEnd?.drop()
                lastTextEnd = mark()
            }
            type = getTokenType()
        }

        rollBackToEol()
        checkNotNull(lastTextEnd)
        lastTextEnd.rollbackTo()
        return DigdagElementTypes.SCALAR_PLAIN_VALUE
    }

    private fun parseExplicitKeyValue(indent: Int): IElementType {
        assert(getTokenType() === QUESTION)

        var indentAddition = getShorthandIndentAddition()
        advanceLexer()

        if (!myStopTokensStack.isEmpty() && myStopTokensStack.peek() == DigdagParser.HASH_STOP_TOKENS // This means we're inside some hash
            && getTokenType() === SCALAR_KEY
        ) {
            parseScalarKeyValue(indent)
        } else {
            myStopTokensStack.add(TokenSet.create(COLON))
            eolSeen = false

            parseBlockNode(indent + indentAddition, false)

            myStopTokensStack.pop()

            passJunk()
            if (getTokenType() === COLON) {
                indentAddition = getShorthandIndentAddition()
                advanceLexer()

                eolSeen = false
                parseBlockNode(indent + indentAddition, false)
            }
        }

        return DigdagElementTypes.KEY_VALUE_PAIR
    }


    private fun parseScalarKeyValue(indent: Int): IElementType {
        assert(getTokenType() === SCALAR_KEY) { "Expected scalar key" }
        eolSeen = false

        val indentAddition = getShorthandIndentAddition()
        advanceLexer()

        return parseSimpleScalarKeyValueFromColon(indent, indentAddition)
    }

    private fun parseSimpleScalarKeyValueFromColon(indent: Int, indentAddition: Int): IElementType {
        assert(getTokenType() === COLON) { "Expected colon" }
        advanceLexer()

        val rollbackMarker = mark()

        passJunk()
        if (eolSeen && (eof() || myIndent + getIndentBonus(false) < indent + indentAddition)) {
            dropEolMarker()
            rollbackMarker.rollbackTo()
        } else {
            dropEolMarker()
            rollbackMarker.rollbackTo()
            parseBlockNode(indent + indentAddition, false)
        }

        return DigdagElementTypes.KEY_VALUE_PAIR
    }

    private fun parseSequenceItem(indent: Int): IElementType {
        assert(getTokenType() === SEQUENCE_MARKER)

        val indentAddition = getShorthandIndentAddition()
        advanceLexer()
        eolSeen = false

        parseBlockNode(indent + indentAddition, true)
        rollBackToEol()
        return DigdagElementTypes.SEQUENCE_ITEM
    }

    private fun parseHash(): IElementType {
        assert(getTokenType() === LBRACE)
        advanceLexer()
        myStopTokensStack.add(DigdagParser.HASH_STOP_TOKENS)

        while (!eof()) {
            if (getTokenType() === RBRACE) {
                advanceLexer()
                break
            }
            parseSingleStatement(0, 0)
        }

        myStopTokensStack.pop()
        dropEolMarker()
        return DigdagElementTypes.HASH
    }

    private fun parseArray(): IElementType {
        assert(getTokenType() === LBRACKET)
        advanceLexer()
        myStopTokensStack.add(DigdagParser.ARRAY_STOP_TOKENS)

        while (!eof()) {
            if (getTokenType() === RBRACKET) {
                advanceLexer()
                break
            }
            if (isJunk()) {
                advanceLexer()
                continue
            }

            val marker = mark()
            val parsedElement = parseSingleStatement(0, 0)
            if (parsedElement != null) {
                marker.done(DigdagElementTypes.SEQUENCE_ITEM)
            } else {
                marker.error(DigdagBundle.message("parsing.error.sequence.item.expected"))
            }

            if (getTokenType() === DigdagTokenTypes.COMMA) {
                advanceLexer()
            }
        }

        myStopTokensStack.pop()
        dropEolMarker()
        return DigdagElementTypes.ARRAY
    }

    private fun eof(): Boolean {
        return myBuilder!!.eof() || myBuilder!!.tokenType === DOCUMENT_MARKER
    }

    private fun getTokenType(): IElementType? {
        return if (eof()) null else myBuilder!!.tokenType
    }

    private fun dropEolMarker() {
        if (myAfterLastEolMarker != null) {
            myAfterLastEolMarker!!.drop()
            myAfterLastEolMarker = null
        }
    }

    private fun rollBackToEol() {
        if (eolSeen && myAfterLastEolMarker != null) {
            eolSeen = false
            myAfterLastEolMarker!!.rollbackTo()
            myAfterLastEolMarker = null
        }
    }

    private fun mark(): PsiBuilder.Marker {
        dropEolMarker()
        return myBuilder!!.mark()
    }

    private fun advanceLexer() {
        if (myBuilder!!.eof()) {
            return
        }
        val type = myBuilder!!.tokenType
        val eolElement: Boolean = DigdagElementTypes.EOL_ELEMENTS.contains(type)
        eolSeen = eolSeen || eolElement
        if (eolElement) {
            // Drop and create new eolMarker
            myAfterLastEolMarker = mark()
            myIndent = 0
        } else if (type === INDENT) {
            myIndent = getCurrentTokenLength()
        } else {
            // Drop Eol Marker if other token seen
            dropEolMarker()
        }
        myBuilder!!.advanceLexer()
    }

    private fun getCurrentTokenLength(): Int {
        return myBuilder!!.rawTokenTypeStart(1) - myBuilder!!.currentOffset
    }

    private fun passJunk() {
        while (!eof() && isJunk()) {
            advanceLexer()
        }
    }

    private fun isJunk(): Boolean {
        val type = getTokenType()
        return type === INDENT || type === EOL
    }

    private fun findLeftRange(tokens: List<IElementType>): Int {
        val i = tokens.indexOf(COMMENT)
        return if (i != -1) i else tokens.size
    }
}