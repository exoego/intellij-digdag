package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.ObjectUtils
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import kotlin.math.min
import net.exoego.intellij.digdag.DigdagGrammarCharUtil
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.DigdagUtil
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagQuotedText


class DigdagQuotedTextImpl(node: ASTNode) : DigdagScalarImpl(node), DigdagQuotedText {
    val myIsSingleQuoted: Boolean

    init {
        val firstContentNode: ASTNode? = getFirstContentNode()
        this.myIsSingleQuoted =
            firstContentNode != null && firstContentNode.getElementType() === DigdagTokenTypes.SCALAR_STRING
    }


    override fun getContentRanges(): List<TextRange> {
        val firstContentNode = getFirstContentNode()
            ?: return emptyList()

        val result: MutableList<TextRange> = ArrayList()
        val contentRange = TextRange.create(firstContentNode.startOffset, textRange.endOffset)
            .shiftRight(-textRange.startOffset)

        val lines = StringUtil.split(
            contentRange.substring(
                text
            ), "\n", false, false
        )
        // First line has opening quote
        var cumulativeOffset = contentRange.startOffset
        for (i in lines.indices) {
            val line = lines[i]

            var lineStart = 0
            var lineEnd = line.length
            if (i == 0) {
                lineStart++
            } else {
                while (lineStart < line.length && DigdagGrammarCharUtil.isSpaceLike(line[lineStart])) {
                    lineStart++
                }
            }
            if (i == lines.size - 1) {
                // Last line has closing quote
                lineEnd--
            } else {
                while (lineEnd > lineStart && DigdagGrammarCharUtil.isSpaceLike(line[lineEnd - 1])) {
                    lineEnd--
                }
            }

            result.add(TextRange.create(lineStart, lineEnd).shiftRight(cumulativeOffset))
            cumulativeOffset += line.length
        }

        return result
    }

    override fun getTextEvaluator(): DigdagQuotedTextTextEvaluator {
        return DigdagQuotedTextTextEvaluator(this)
    }

    override fun getDecodeReplacements(input: CharSequence): List<Pair<TextRange, String>> {
        val result: MutableList<Pair<TextRange, String>> = ArrayList()

        var i = 0
        while (i + 1 < input.length) {
            if (isSingleQuote() && input[i] == '\'' && input[i + 1] == '\'') {
                result.add(Pair.create(TextRange.from(i, 2), "'"))
                i++
            } else if (!isSingleQuote() && input[i] == '\\') {
                if (input[i + 1] == '\n') {
                    result.add(Pair.create(TextRange.from(i, 2), if (i > 0 || input.length > i + 2) "" else "\n"))
                    i++
                    ++i
                    continue
                }
                val length = Escaper.findEscapementLength(input, i)
                val charCode = Escaper.toUnicodeChar(input, i, length)
                val range = TextRange.create(
                    i,
                    min((i + length + 1).toDouble(), input.length.toDouble()).toInt()
                )
                result.add(Pair.create(range, Character.toString(charCode.toChar())))
                i += range.length - 1
            }
            ++i
        }
        return result
    }

    @Throws(IllegalArgumentException::class)
    override fun getEncodeReplacements(input: CharSequence): List<Pair<TextRange, String>> {
        // check for consistency
        if (isSingleQuote()) {
            for (i in input.indices) {
                require(
                    !(input[i] == '\n' && !isSurroundedByNoSpace(
                        input,
                        i
                    ))
                ) { "Newlines with spaces around are not convertible" }
            }
        }

        val indent: Int = DigdagUtil.getIndentToThisElement(this)
        val indentString = StringUtil.repeatSymbol(' ', indent)

        val result: MutableList<Pair<TextRange, String>> = ArrayList()
        var currentLength = 0
        for (i in input.indices) {
            val c = input[i]
            if (c == '\n') {
                if (!isSingleQuote() && i + 1 < input.length && DigdagGrammarCharUtil.isSpaceLike(input[i + 1])) {
                    result.add(
                        Pair.create(
                            TextRange.from(i, 1),
                            "\\n\\\n$indentString\\"
                        )
                    )
                } else if (!isSingleQuote() && i + 1 < input.length && input[i + 1] == '\n' && i > 0) {
                    result.add(
                        Pair.create(
                            TextRange.from(i, 1),
                            "\\\n$indentString\\n"
                        )
                    )
                } else {
                    result.add(
                        Pair.create(
                            TextRange.from(i, 1),
                            """
                        
                        $indentString
                        """.trimIndent()
                        )
                    )
                }
                currentLength = 0
                continue
            }


            if (currentLength > MAX_SCALAR_LENGTH_PREDEFINED
                && (!isSingleQuote() || (c == ' ' && isSurroundedByNoSpace(input, i)))
            ) {
                val replacement = if (isSingleQuote()) {
                    """
     
     $indentString
     """.trimIndent()
                } else if (DigdagGrammarCharUtil.isSpaceLike(c)) {
                    """
     \
     $indentString\
     """.trimIndent()
                } else {
                    """
     \
     $indentString
     """.trimIndent()
                }
                result.add(Pair.create(TextRange.from(i, if (isSingleQuote()) 1 else 0), replacement))
                currentLength = 0
            }

            currentLength++

            if (isSingleQuote() && c == '\'') {
                result.add(Pair.create(TextRange.from(i, 1), "''"))
                continue
            }

            if (!isSingleQuote()) {
                if (c == '"') {
                    result.add(Pair.create(TextRange.from(i, 1), "\\\""))
                } else if (c == '\\') {
                    result.add(Pair.create(TextRange.from(i, 1), "\\\\"))
                }
            }
        }
        return result
    }

    override fun isMultiline(): Boolean {
        return textContains('\n')
    }

    override fun isSingleQuote(): Boolean {
        return myIsSingleQuoted
    }

    override fun toString(): String {
        return "Digdag quoted text"
    }

    object Escaper {
        private val ONE_LETTER_CONVERSIONS = arrayOf(
            intArrayOf('0'.code, 0),
            intArrayOf('a'.code, 7),
            intArrayOf('b'.code, 8),
            intArrayOf('t'.code, 9),
            intArrayOf(9, 9),
            intArrayOf('n'.code, 10),
            intArrayOf('v'.code, 11),
            intArrayOf('f'.code, 12),
            intArrayOf('r'.code, 13),
            intArrayOf('e'.code, 27),
            intArrayOf(' '.code, 32),
            intArrayOf('"'.code, 34),
            intArrayOf('/'.code, 47),
            intArrayOf('\\'.code, 92),
            intArrayOf('N'.code, 133),
            intArrayOf('_'.code, 160),
            intArrayOf('L'.code, 8232),
            intArrayOf('P'.code, 8233),
        )

        private val ESC_TO_CODE = NotNullLazyValue.createValue {
            val map: Int2IntMap =
                Int2IntOpenHashMap(ONE_LETTER_CONVERSIONS.size)
            for (conversion in ONE_LETTER_CONVERSIONS) {
                map.put(conversion[0], conversion[1])
            }
            map
        }

        fun findEscapementLength(text: CharSequence, pos: Int): Int {
            require(!(pos + 1 >= text.length || text[pos] != '\\')) { "This is not an escapement start" }

            val c = text[pos + 1]
            return when (c) {
                'x' -> 3
                'u' -> 5
                'U' -> 9
                else -> 1
            }
        }

        fun toUnicodeChar(text: CharSequence, pos: Int, length: Int): Int {
            if (length > 1) {
                val s = text.subSequence(
                    pos + 2,
                    min(text.length.toDouble(), (pos + length + 1).toDouble()).toInt()
                )
                return try {
                    s.toString().toInt(16)
                } catch (e: NumberFormatException) {
                    '?'.code
                }
            } else {
                val result = ESC_TO_CODE.value[text[pos + 1].code]
                return ObjectUtils.notNull(result, text[pos + 1].code)
            }
        }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitQuotedText(this)
        } else {
            super.accept(visitor)
        }
    }
}