package net.exoego.intellij.digdag

import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

object DigdagElementTypes {
    val DOCUMENT: DigdagElementType = DigdagElementType("Document ---")

    val KEY_VALUE_PAIR: DigdagElementType = DigdagElementType("Key value pair")

    //DigdagElementType VALUE = new DigdagElementType("Value");
    val HASH: DigdagElementType = DigdagElementType("Hash")
    val ARRAY: DigdagElementType = DigdagElementType("Array")
    val SEQUENCE_ITEM: DigdagElementType = DigdagElementType("Sequence item")
    val COMPOUND_VALUE: DigdagElementType = DigdagElementType("Compound value")
    val MAPPING: DigdagElementType = DigdagElementType("Mapping")
    val SEQUENCE: DigdagElementType = DigdagElementType("Sequence")
    val SCALAR_LIST_VALUE: DigdagElementType = DigdagElementType("Scalar list value")
    val SCALAR_TEXT_VALUE: DigdagElementType = DigdagElementType("Scalar text value")
    val SCALAR_PLAIN_VALUE: DigdagElementType = DigdagElementType("Scalar plain style")
    val SCALAR_QUOTED_STRING: DigdagElementType = DigdagElementType("Scalar quoted string")
    val ANCHOR_NODE: DigdagElementType = DigdagElementType("Anchor node")
    val ALIAS_NODE: DigdagElementType = DigdagElementType("Alias node")

    val BLOCK_SCALAR_ITEMS: TokenSet = TokenSet.create(
        DigdagTokenTypes.SCALAR_LIST,
        DigdagTokenTypes.SCALAR_TEXT
    )

    val TEXT_SCALAR_ITEMS: TokenSet = TokenSet.create(
        DigdagTokenTypes.SCALAR_STRING,
        DigdagTokenTypes.SCALAR_DSTRING,
        DigdagTokenTypes.TEXT
    )

    val SCALAR_ITEMS: TokenSet = TokenSet.orSet(BLOCK_SCALAR_ITEMS, TEXT_SCALAR_ITEMS)

    val SCALAR_VALUES: TokenSet = TokenSet.orSet(
        SCALAR_ITEMS, TokenSet.create(
            SCALAR_LIST_VALUE
        )
    )

    val EOL_ELEMENTS: TokenSet = TokenSet.create(
        DigdagTokenTypes.EOL,
        DigdagTokenTypes.SCALAR_EOL
    )

    val SPACE_ELEMENTS: TokenSet = TokenSet.orSet(
        EOL_ELEMENTS, TokenSet.create(
            DigdagTokenTypes.WHITESPACE,
            TokenType.WHITE_SPACE,
            DigdagTokenTypes.INDENT
        )
    )

    val BLANK_ELEMENTS: TokenSet = TokenSet.orSet(
        SPACE_ELEMENTS, TokenSet.create(
            DigdagTokenTypes.COMMENT
        )
    )

    val CONTAINERS: TokenSet = TokenSet.create(
        SCALAR_LIST_VALUE,
        SCALAR_TEXT_VALUE,
        DOCUMENT,
        SEQUENCE,
        MAPPING,
        SCALAR_QUOTED_STRING,
        SCALAR_PLAIN_VALUE
    )

    val BRACKETS: TokenSet = TokenSet.create(
        DigdagTokenTypes.LBRACE,
        DigdagTokenTypes.RBRACE,
        DigdagTokenTypes.LBRACKET,
        DigdagTokenTypes.RBRACKET
    )

    val DOCUMENT_BRACKETS: TokenSet = TokenSet.create(
        DigdagTokenTypes.DOCUMENT_MARKER,
        DigdagTokenTypes.DOCUMENT_END
    )

    val TOP_LEVEL: TokenSet = TokenSet.create(
        DigdagParserDefinition.FILE,
        DOCUMENT
    )

    val INCOMPLETE_BLOCKS: TokenSet = TokenSet.create(
        MAPPING,
        SEQUENCE,
        COMPOUND_VALUE,
        SCALAR_LIST_VALUE,
        SCALAR_TEXT_VALUE
    )

    val YAML_COMMENT_TOKENS: TokenSet = TokenSet.create(DigdagTokenTypes.COMMENT)

    val WHITESPACE_TOKENS: TokenSet = TokenSet.create(DigdagTokenTypes.WHITESPACE)
}