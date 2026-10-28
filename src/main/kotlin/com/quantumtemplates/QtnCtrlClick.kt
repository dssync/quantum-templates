package com.quantumtemplates

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionWrapper
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList

// Ctrl+Click (and Ctrl+B) run the GotoDeclaration action, which Rider forwards to the ReSharper backend, so the
// frontend can't add a target to Rider's own chooser. Instead GotoDeclaration is wrapped: when the symbol has a
// QTN declaration the wrapper offers it (or goes straight there, see QuantumSettings) and otherwise hands
// the click to Rider unchanged.
class QtnGotoDeclarationWrapper(original: AnAction) : AnActionWrapper(original) {

    // Set when "Generated C# Declaration" is chosen. Rider's action re-dispatches through the GotoDeclaration id,
    // which lands back here, so the next invocation at the same caret goes straight to Rider.
    private var bypass: Bypass? = null

    private class Bypass(val editor: Editor, val offset: Int, val expiresAt: Long)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val mode = QuantumSettings.getInstance().ctrlClickMode
        if (project == null || editor == null || mode == QtnCtrlClickMode.OFF || !isCSharp(editor)
            || DumbService.isDumb(project) || consumeBypass(editor)
        ) {
            return super.actionPerformed(e)
        }

        val offset = QtnLookup.targetOffset(editor, e.inputEvent)
        val target = QtnLookup.identifierAt(editor.document.charsSequence, offset)
            ?.let { QtnLookup.find(project, it) }
            ?: return super.actionPerformed(e)

        when (mode) {
            QtnCtrlClickMode.QTN -> target.navigate(project)
            else -> showChooser(project, editor, offset, target)
        }
    }

    private fun consumeBypass(editor: Editor): Boolean {
        val pending = bypass ?: return false
        bypass = null
        return pending.editor == editor && pending.offset == editor.caretModel.offset
            && System.currentTimeMillis() <= pending.expiresAt
    }

    private fun isCSharp(editor: Editor) =
        FileDocumentManager.getInstance().getFile(editor.document)?.extension.equals("cs", ignoreCase = true)

    private fun showChooser(project: Project, editor: Editor, offset: Int, target: QtnDeclaration) {
        // Ctrl+Click doesn't move the caret, but the delegated C# navigation runs without the original mouse
        // event and so acts on the caret.
        editor.caretModel.moveToOffset(offset)

        val generated = "Generated C# Declaration"
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(listOf<Any>(target, generated))
            .setTitle("Declarations of '${target.word}'")
            .setRenderer(object : SimpleListCellRenderer<Any>() {
                override fun customize(list: JList<out Any>, value: Any?, index: Int, selected: Boolean, hasFocus: Boolean) {
                    if (value is QtnDeclaration) {
                        text = "QTN File (${value.file.name})"
                        icon = QuantumIcons.QuantumIcon
                    } else {
                        text = generated
                        icon = AllIcons.Nodes.Class
                    }
                }
            })
            .setItemChosenCallback { choice ->
                if (choice is QtnDeclaration) {
                    choice.navigate(project)
                } else {
                    // Let the popup close and focus return to the editor before Rider takes over.
                    ApplicationManager.getApplication().invokeLater {
                        bypass = Bypass(editor, editor.caretModel.offset, System.currentTimeMillis() + BYPASS_WINDOW_MS)
                        ActionManager.getInstance().tryToExecute(
                            delegate, null, editor.contentComponent, ActionPlaces.KEYBOARD_SHORTCUT, true
                        )
                    }
                }
            }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private companion object {
        // Covers a round trip to the backend; a stale bypass can only ever skip one popup at the same caret.
        const val BYPASS_WINDOW_MS = 3000L
    }
}

// Installs the wrapper once Rider has registered its own GotoDeclaration, and puts the original back if the plugin
// is unloaded (the service is disposed with the plugin).
@Service(Service.Level.APP)
class QtnCtrlClickInstaller : Disposable {

    @Synchronized
    fun install() {
        val actionManager = ActionManager.getInstance()
        val current = actionManager.getAction(ACTION_ID) ?: return
        if (current is QtnGotoDeclarationWrapper) return
        actionManager.replaceAction(ACTION_ID, QtnGotoDeclarationWrapper(current))
    }

    @Synchronized
    override fun dispose() {
        val actionManager = ActionManager.getInstance()
        val current = actionManager.getAction(ACTION_ID) as? QtnGotoDeclarationWrapper ?: return
        actionManager.replaceAction(ACTION_ID, current.delegate)
    }

    private companion object {
        const val ACTION_ID = "GotoDeclaration"
    }
}

class QtnCtrlClickStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        service<QtnCtrlClickInstaller>().install()
    }
}
