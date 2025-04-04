package net.exoego.intellij.digdag.action

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog.Builder
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import net.exoego.intellij.digdag.DigdagFileType
import net.exoego.intellij.digdag.DigdagIcons
import com.intellij.openapi.diagnostic.thisLogger

class DigdagCreateFileAction :
    CreateFileFromTemplateAction("Digdag File", "Create new Digdag file", DigdagFileType.icon) {

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: Builder) {
        builder
            .setTitle("Digdag File")
            .addKind("Digdag file", DigdagIcons.FILE, "Digdag File")
    }

    override fun getActionName(
        directory: PsiDirectory,
        newName: String,
        templateName: String
    ): String = "Digdag File"

    override fun createFileFromTemplate(
        name: String,
        template: FileTemplate,
        dir: PsiDirectory
    ): PsiFile? =
        try {
            val project = dir.project
            val templateManager = FileTemplateManager.getInstance(project)
            val properties = templateManager.defaultProperties
            val dialog =
                CreateFromTemplateDialog(
                    project,
                    dir,
                    template,
                    AttributesDefaults(name).withFixedName(true),
                    properties
                )
            dialog.create().containingFile
        } catch (e: Exception) {
            thisLogger().error("Error creating new Digdag file", e)
            null
        }

}