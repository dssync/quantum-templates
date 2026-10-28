package com.quantumtemplates

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

// Text, description and icon are declared in plugin.xml.
class NewQuantumFileAction : CreateFileFromTemplateAction() {

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder
            .setTitle("New Quantum File")
            .addKind("Component (.qtn)",                    QuantumIcons.QuantumIcon, "QuantumComponent")
            .addKind("System (Filter)",                     QuantumIcons.QuantumIcon, "QuantumSystemFilter")
            .addKind("System (Signals Only)",               QuantumIcons.QuantumIcon, "QuantumSystemSignalsOnly")
            .addKind("System",                              QuantumIcons.QuantumIcon, "QuantumSystem")
            .addKind("Command",                             QuantumIcons.QuantumIcon, "QuantumCommand")
            .addKind("Asset Script",                        QuantumIcons.QuantumIcon, "QuantumAsset")
            .addKind("Entity View Context",                 QuantumIcons.QuantumIcon, "QuantumEntityViewContext")
            .addKind("Entity View Component",               QuantumIcons.QuantumIcon, "QuantumEntityViewComponent")
            .addKind("Entity View Component With Context",  QuantumIcons.QuantumIcon, "QuantumEntityViewComponentWithContext")
            .addKind("Input Script",                        QuantumIcons.QuantumIcon, "QuantumInputScript")
    }

    override fun createFile(name: String, templateName: String, dir: PsiDirectory): PsiFile? {
        val templateManager = FileTemplateManager.getInstance(dir.project)
        // Converted on a copy so the registered template is left as written.
        val template: FileTemplate = templateManager.getInternalTemplate(templateName).clone().apply {
            text = BraceStyle.apply(text, QuantumSettings.getInstance().braceStyle)
        }

        val props = templateManager.defaultProperties
        props.setProperty(FileTemplate.ATTRIBUTE_NAME, name)
        props.setProperty("NAMESPACE", resolveNamespace(dir))

        return FileTemplateUtil.createFromTemplate(template, name, props, dir).containingFile
    }

    override fun getActionName(
        directory: PsiDirectory?,
        newName: String,
        templateName: String?
    ): String = "Create Quantum File: $newName"

    /**
     * Builds a namespace from the folders below the nearest "root" folder,
     * e.g. Assets/Game/View -> Game.View. Falls back to "Quantum".
     */
    private fun resolveNamespace(directory: PsiDirectory): String {
        val projectDir = directory.project.guessProjectDir()
        val parts = mutableListOf<String>()
        var current: PsiDirectory? = directory
        while (current != null
            && current.name !in NAMESPACE_ROOTS
            && current.virtualFile != projectDir
        ) {
            parts.add(0, toIdentifier(current.name))
            current = current.parentDirectory
        }
        return if (parts.isEmpty()) "Quantum" else parts.joinToString(".")
    }

    /** Makes a folder name a valid C# identifier, e.g. "My-Views" -> "My_Views", "2D" -> "_2D". */
    private fun toIdentifier(folderName: String): String {
        val cleaned = folderName.replace(Regex("[^A-Za-z0-9_]"), "_")
        return if (cleaned.first().isDigit()) "_$cleaned" else cleaned
    }

    private companion object {
        val NAMESPACE_ROOTS = setOf("Scripts", "Simulation", "QuantumUser", "Assets", "src")
    }
}
