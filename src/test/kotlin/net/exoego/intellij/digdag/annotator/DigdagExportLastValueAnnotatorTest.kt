package net.exoego.intellij.digdag.annotator

class DigdagExportLastValueAnnotatorTest : DigdagAnnotatorTestCase() {
    override fun getTestDataPath(): String = "src/test/resources/annotator"

    fun testExportLastValueHttp() {
        checkHighlighting("export_last_value_http.dig")
    }

    fun testExportLastValueTd() {
        checkHighlighting("export_last_value_td.dig")
    }
}
