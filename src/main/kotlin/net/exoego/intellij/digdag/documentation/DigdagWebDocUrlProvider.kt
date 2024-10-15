package net.exoego.intellij.digdag.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.util.function.Consumer
import net.exoego.intellij.digdag.psi.impl.DigdagKeyValueImpl
import net.exoego.intellij.digdag.psi.impl.DigdagMappingImpl

data class Content(val operator: String, val title: String, val body: String, val link: String)

class DigdagWebDocUrlProvider : AbstractDocumentationProvider() {
    override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement): String? =
        buildString {
            return null
        }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val (name, value) =
            when (element) {
                is DigdagKeyValueImpl -> {
                    Pair(element.name, element.getValueText())
                }

                is DigdagMappingImpl -> {
                    Pair(element.name, "TODO")
                }

                else -> {
                    return null
                }
            }

        val content: Content = when (name) {
            "call>" -> {
                Content(
                    "call",
                    "Calls another workflow",
                    "This operator embeds another workflow as a subtask. This operator waits until all tasks of the workflow complete.",
                    "https://docs.digdag.io/operators/call.html"
                )
            }

            "require>" -> {
                Content(
                    "require",
                    "Depends on another workflow",
                    """This operator requires completion of another workflow.
                        
This operator is similar to <codE>call></code> operator, but this operator doesn’t start the other workflow if it’s already running or has done for the same session time of this workflow.

If the workflow is running or newly started, this operator waits until it completes.

In addition, <code>require</codE> operator can kick the another project’s workflow.
                    """.trimMargin(),
                    "https://docs.digdag.io/operators/require.html"
                )
            }

            "http_call>" -> {
                Content(
                    "http_call",
                    "Treasure Data queries",
                    """This operator makes a HTTP request, parse response body as workflow, and embeds it as a subtask. The operator is similar to call> operator. The difference is that another workflow is fetched from HTTP.

This operator parses response body based on returned Content-Type header. Content-Type must be set and following values are supported:

<ul>
<li><code>application/json</code>: Parse the response as JSON.</li>
<li><code>application/x-yaml</code>: Use the returned body as-is.</li>
</ul>

If appropriate Content-Type header is not returned, use content_type_override option.
""",
                    "https://docs.digdag.io/operators/td.html"
                )
            }

            "td>" -> {
                Content(
                    "td",
                    "Call workflow fetched by HTTP",
                    """This operator runs a Hive or Presto query on Treasure Data.

""",
                    "https://docs.digdag.io/operators/http_call.html"
                )
            }


            "sh>" -> {
                Content(
                    "sh",
                    "Shell scriptsP",
                    """This operator runs a shell script.

Running a shell command (Note: you can use <a href="https://docs.digdag.io/operators/echo.html">echo> operator</a> to show a message):
""",
                    "https://docs.digdag.io/operators/sh.html"
                )
            }

            else -> {
                return null
            }
        }

        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append("${content.operator}>: ${content.title}")
            append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START)
            append(content.body)
            append("<h4><a target=\"_blank\" href=\"${content.link}\">Operator <code>${name}</code> on docs.digdag.io ${DocumentationMarkup.EXTERNAL_LINK_ICON}</a></h4>")
            append(DocumentationMarkup.CONTENT_END)
        }
    }

    override fun generateRenderedDoc(element: PsiDocCommentBase): String {
        return "aaaa"
    }

    override fun collectDocComments(file: PsiFile, sink: Consumer<in PsiDocCommentBase>) {
    }

    override fun getDocumentationElementForLink(
        psiManager: PsiManager,
        link: String,
        position: PsiElement
    ): PsiElement? {
        return null
    }
}