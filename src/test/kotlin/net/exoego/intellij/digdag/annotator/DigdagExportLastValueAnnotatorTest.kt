package net.exoego.intellij.digdag.annotator

class DigdagExportLastValueAnnotatorTest : DigdagAnnotatorTestCase() {
    override fun getTestDataPath(): String = "src/test/resources/annotator"

    fun testExportLastValue() {
        checkHighlighting("export_last_value.dig")
    }
}
