package net.exoego.intellij.digdag.annotator

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestIndexingModeSupporter
import com.intellij.testFramework.TestIndexingModeSupporter.IndexingMode
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory


abstract class DigdagAnnotatorTestCase: UsefulTestCase(), TestIndexingModeSupporter {
    abstract fun getTestDataPath(): String

    private lateinit var indexingMode: IndexingMode

    override fun setIndexingMode(mode: IndexingMode) {
        this.indexingMode = mode
    }

    override fun getIndexingMode(): IndexingMode {
        return this.indexingMode
    }

    protected lateinit var codeInsightTestFixture: CodeInsightTestFixture

    override fun setUp() {
        super.setUp()

        val fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixture =
            fixtureFactory
                .createLightFixtureBuilder(
                    LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR,
                    "sample digdag project"
                )
                .fixture
        codeInsightTestFixture = fixtureFactory.createCodeInsightFixture(fixture)
        codeInsightTestFixture.testDataPath = getTestDataPath()
        codeInsightTestFixture.setUp()
    }

    fun checkHighlighting(file: String) {
        codeInsightTestFixture.configureByFile(file)
        codeInsightTestFixture.checkHighlighting()
    }

    override fun tearDown() {
        try {
            codeInsightTestFixture.tearDown()
        } finally {
            super.tearDown()
        }
    }
}
