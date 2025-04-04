package net.exoego.intellij.digdag.annotator

class DigdagUnknownOperatorAnnotatorTest : DigdagAnnotatorTestCase() {
    override fun getTestDataPath(): String = "src/test/resources/annotator"

    fun testUnknownOperator() {
        checkHighlighting("unknown_operator.dig")
    }
}
