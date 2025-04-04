package net.exoego.intellij.digdag.annotator

class DigdagInvalidBlockChildrenErrorAnnotatorTest : DigdagAnnotatorTestCase() {
    override fun getTestDataPath(): String = "src/test/resources/annotator"

    fun testUnknownOperator() {
        checkHighlighting("invalid_block_children.dig")
    }
}
