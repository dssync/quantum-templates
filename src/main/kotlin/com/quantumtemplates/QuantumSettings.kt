package com.quantumtemplates

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel

/** What Ctrl+Click (Go to Declaration) does on a C# symbol that has a QTN declaration. */
enum class QtnCtrlClickMode {
    /** Show a popup offering the .qtn declaration or the generated C#. */
    ASK,
    /** Go straight to the .qtn declaration. */
    QTN,
    /** Leave Rider's Go to Declaration untouched. */
    OFF,
}

/** Where new Quantum files put their opening braces. The templates are written K&R (see BraceStyle.kt). */
enum class TemplateBraceStyle {
    /** `class Foo {` */
    K_AND_R,
    /** `class Foo` with `{` on the next line. */
    ALLMAN,
}

@Service(Service.Level.APP)
@State(name = "QuantumTemplates", storages = [Storage("quantumTemplates.xml")])
class QuantumSettings : SimplePersistentStateComponent<QuantumSettings.Options>(Options()) {

    class Options : BaseState() {
        var ctrlClickMode by enum(QtnCtrlClickMode.ASK)
        var braceStyle by enum(TemplateBraceStyle.K_AND_R)
    }

    var ctrlClickMode: QtnCtrlClickMode
        get() = state.ctrlClickMode
        set(value) { state.ctrlClickMode = value }

    var braceStyle: TemplateBraceStyle
        get() = state.braceStyle
        set(value) { state.braceStyle = value }

    companion object {
        fun getInstance(): QuantumSettings = service()
    }
}

// Settings > Tools > Quantum. Registered in plugin.xml.
class QuantumSettingsConfigurable : BoundConfigurable("Quantum") {

    override fun createPanel(): DialogPanel {
        val settings = QuantumSettings.getInstance()
        return panel {
            buttonsGroup("Ctrl+Click on a symbol declared in a .qtn file:") {
                row { radioButton("Ask: QTN declaration or generated C#", QtnCtrlClickMode.ASK) }
                row { radioButton("Go to the QTN declaration", QtnCtrlClickMode.QTN) }
                row { radioButton("Go to the generated C# (Rider default)", QtnCtrlClickMode.OFF) }
            }.bind(settings::ctrlClickMode)
            row {
                comment("Go to QTN Declaration (Ctrl+Alt+Q by default) works in every mode.")
            }
            buttonsGroup("Brace style for new Quantum files:") {
                row { radioButton("K&R: opening brace on the same line", TemplateBraceStyle.K_AND_R) }
                row { radioButton("Allman: opening brace on its own line", TemplateBraceStyle.ALLMAN) }
            }.bind(settings::braceStyle)
        }
    }
}
