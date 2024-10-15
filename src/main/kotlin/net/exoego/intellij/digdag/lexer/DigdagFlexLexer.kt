package net.exoego.intellij.digdag.lexer

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.MergingLexerAdapter
import com.intellij.psi.tree.TokenSet
import net.exoego.intellij.digdag.DigdagTokenTypes

private val TOKENS_TO_MERGE: TokenSet = TokenSet.create(DigdagTokenTypes.TEXT)
private const val DIRTY_STATE = 239;

class DigdagFlexLexer : MergingLexerAdapter(MyFlexAdapter(_DigdagLexer()), TOKENS_TO_MERGE)

private class MyFlexAdapter(flex: _DigdagLexer) : FlexAdapter(flex) {
    override fun getFlex(): _DigdagLexer {
        return super.getFlex() as _DigdagLexer
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        var initialState = initialState
        if (initialState != DIRTY_STATE) {
            this.flex.cleanMyState()
        } else {
            // That should not occur normally, but some complex lexers (e.g. black and white lexer)
            // require "suspending" of the lexer to pass some template language. In these cases we
            // believe that the same instance of the lexer would be restored (with its internal state)
            initialState = 0
        }

        super.start(buffer, startOffset, endOffset, initialState)
    }

    override fun getState(): Int {
        val state = super.getState()
        if (state != 0 || this.flex.isCleanState()) {
            return state
        }
        return DIRTY_STATE
    }
}