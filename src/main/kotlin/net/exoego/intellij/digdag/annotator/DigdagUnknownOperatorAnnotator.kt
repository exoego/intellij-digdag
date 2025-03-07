package net.exoego.intellij.digdag.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.findTopmostParentOfType
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagMapping

class DigdagUnknownOperatorAnnotator : BaseAnnotator() {
    private val builtinOperators: Set<String> = setOf(
        "call>",
        "http_call>",
        "require>",
        "loop>",
        "for_each>",
        "for_range>",
        "if>",
        "fail>",
        "echo>",
        "wait>",
        "td>",
        "td_run>",
        "td_ddl>",
        "td_load>",
        "td_for_each>",
        "td_wait>",
        "td_wait_table>",
        "td_partial_delete>",
        "td_table_export>",
        "td_result_export>",
        "pg>",
        "mail>",
        "http>",
        "s3_wait>",
        "redshift>",
        "redshift_load>",
        "redshift_unload>",
        "emr>",
        "gcs_wait>",
        "bq>",
        "bq_ddl>",
        "bq_extract>",
        "bq_load>",
        "sh>",
        "py>",
        "rb>",
        "embulk>",
        "param_get>",
        "param_set>",
    )

    // https://docs.digdag.io/community_contributions.html
    private val communityOperators: Map<String, Set<String>> = mapOf(
        "com.github.szyn:digdag-slack" to setOf("slack>"),
        "io.github.retz:retz-digdag-plugin" to setOf("retz_run>"),
        "com.github.hiroyuki-sato:digdag-plugin-ssh" to setOf("ssh>"),
        "com.github.tamanyan:digdag-hangouts-chat" to setOf("hangouts>"),
        "com.github.hiroyuki-sato:digdag-plugin-mysql" to setOf("mysql>"),
        "com.github.kimutansk:digdag-plugin-dfs_wait" to setOf("dfs_wait>"),
        "jp.co.septeni_original:k8sop_2.12" to setOf("k8s_job"),
        "pro.civitaspo:digdag-operator-emr_fleet" to setOf(
            "emr_fleet.create_cluster>",
            "emr_fleet.shutdown_cluster>",
            "emr_fleet.wait_cluster>",
        ),
        "pro.civitaspo:digdag-operator-athena" to setOf(
            "athena.query>",
            "athena.add_partition>",
            "athena.drop_partition>",
            "athena.partition_exists?>",
            "athena.apas>",
            "athena.preview>",
            "athena.ctas>",
            "athena.drop_table>",
            "athena.drop_table_multi>",
            "athena.table_exists?>",
            "athena.each_database>",
        ),
        "com.github.takemikami:digdag-plugin-shresult" to setOf("sh_result>"),
        "pro.civitaspo:digdag-operator-cost_explorer" to setOf("cost_explorer.get_cost>"),
        "pro.civitaspo:digdag-operator-param" to setOf("param_store>", "param_reset>", "param_eval>"),
        "pro.civitaspo:digdag-operator-ecs_task" to setOf(
            "ecs_task.run>",
            "ecs_task.register>",
            "ecs_task.wait>",
            "ecs_task.result>",
            "ecs_task.py>",
            "ecs_task.rb>",
            "ecs_task.sh>",
            "ecs_task.embulk>",
        ),
        "com.github.kencharos:digdag-plugin-azure" to setOf(
            "storage_queue_wait>",
            "blob_wait>",
        ),
        "com.github.kulmam92:digdag-plugin-mssql" to setOf("mssql>"),
        "pro.civitaspo:digdag-operator-livy" to setOf("livy.submit_job>", "livy.wait_job>"),
        "com.github.yuhiwa:digdag-plugin-sshresult" to setOf("ssh_result>"),
        "dev.nomadblacky:digdag-plugin-datadog_2.13" to setOf("datadog_event>"),
        "com.github.supimen:digdag-plugin-glue" to setOf("glue.start_crawler>", "glue.start_job_run>"),
        "pro.civitaspo:digdag-operator-pg_lock" to setOf("pg_lock>"),
        "com.github.katsuyan:digdag-plugin-myjdbc" to setOf("myjdbc>"),
        "com.dena.digdag:digdag-operator-bq-wait" to setOf("bq_wait>"),
        "com.github.emanon-was:digdag-operator-aws-appconfig" to setOf("aws.appconfig.get_configuration>"),
    )

    private fun findCustomOperators(element: PsiElement): Set<String> {
        return element.findTopmostParentOfType<DigdagMapping>()
            ?.getChildMapping("_export")
            ?.getChildMapping("plugin")
            ?.getChildSequence("dependencies")
            ?.getItems()
            ?.map { it.getValue()?.text?.substringBeforeLast(":")?.trim() ?: "" }
            ?.flatMap { communityOperators[it] ?: emptySet() }
            ?.toSet()
            ?: emptySet()
    }

    private val customOperatorsByFile: MutableMap<PsiFile, Set<String>> = mutableMapOf()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is DigdagKeyValue) return

        val key = element.getKey() ?: return

        val isOperator = key.text.endsWith(">")
        if (!isOperator || builtinOperators.contains(key.text)) return

        val customOperators = customOperatorsByFile.computeIfAbsent(key.containingFile) {
            findCustomOperators(key)
        }
        if (customOperators.contains(key.text)) return

        holder
            .newAnnotation(HighlightSeverity.WARNING, "Unknown operator")
            .range(key)
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .create()
    }
}
