package net.exoego.intellij.digdag.parser

import com.intellij.testFramework.ParsingTestCase
import net.exoego.intellij.digdag.DigdagParserDefinition

 class DigdagParserTest : ParsingTestCase("", "dig", DigdagParserDefinition()) {
     override fun skipSpaces(): Boolean = false
     override fun includeRanges(): Boolean = true
     override fun getTestDataPath(): String = "src/test/resources/parser"

     fun testBase() {
         doCodeTest(
             """ 
timezone: UTC

+step1:
  sh>: tasks/shell_sample.sh
  param1: "this is param1"
                 """.trimIndent()
         )
     }
}
