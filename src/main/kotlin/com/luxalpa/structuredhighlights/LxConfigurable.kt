package com.luxalpa.structuredhighlights

import com.intellij.ide.DataManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.bindValue
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class LxConfigurable : Configurable, Configurable.NoScroll, Configurable.NoMargin {
    var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = MyBundle.message("pluginName")

    override fun createComponent(): JComponent {
        com.intellij.lang.Language.getRegisteredLanguages()
            .sortedBy { it.id }
            .forEach { LOGGER.warn("${it.id} | ${it.displayName}") }

        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.getPanel()
    }

    override fun getPreferredFocusedComponent(): JComponent? = null

    override fun isModified(): Boolean {
        return mySettingsComponent!!.dialogPanel.isModified()
    }

    override fun apply() {
        debug { "Applying settings" }
        mySettingsComponent?.dialogPanel?.apply()
    }

    override fun reset() {
        debug { "Resetting settings" }
        mySettingsComponent?.dialogPanel?.reset()
    }

    override fun disposeUIResources() {
        debug { "Disposing UI resources" }
        mySettingsComponent = null
        LxApplicationSettings.instance.previewSettings.reset()
    }

    override fun cancel() {
        debug { "Cancelling settings" }
    }
}

class AppSettingsComponent {
    val myMainPanel: JPanel
    val textField: LxLanguageTextField
    val dialogPanel: DialogPanel
    val previewSettings: PreviewSettings

    init {
        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        previewSettings = LxApplicationSettings.instance.previewSettings
        previewSettings.reset()

        val (previewText, languageId) = LanguageSupport.EP.extensionList.firstOrNull()?.let {
            it.previewText to it.languageId
        } ?: ("No preview text available." to "")

        textField = LxLanguageTextField(project, previewText, languageId)

        myMainPanel = JPanel(BorderLayout())

        val leftPanel = panel {
            row {
                link("Configure highlight colors…") {
                    val settings = Settings.KEY.getData(
                        DataManager.getInstance().getDataContext(myMainPanel)
                    ) ?: return@link

                    val colors =
                        settings.find("reference.settingsdialog.IDE.editor.colors.Structured Highlights") ?: return@link
                    settings.select(colors)
                }
            }
            group("Opacity") {
                row("Normal:") {
                    spinner(0.0..1.0, 0.005).bindValue(
                        LxApplicationSettings.instance::opacityNormal
                    ).applyToComponent {
                        addChangeListener {
                            previewSettings.opacityNormal = value as Double
                            refresh()
                        }
                    }
                }
                row("Header:") {
                    spinner(0.0..1.0, 0.005).bindValue(
                        LxApplicationSettings.instance::opacityHeader
                    ).applyToComponent {
                        addChangeListener {
                            previewSettings.opacityHeader = value as Double
                            refresh()
                        }
                    }
                }
                row("Subheader:") {
                    spinner(0.0..1.0, 0.005).bindValue(
                        LxApplicationSettings.instance::opacitySubheader
                    ).applyToComponent {
                        addChangeListener {
                            previewSettings.opacitySubheader = value as Double
                            refresh()
                        }
                    }
                }
                row("Identifier:") {
                    spinner(0.0..1.0, 0.005).bindValue(
                        LxApplicationSettings.instance::opacityIdentifier
                    ).applyToComponent {
                        addChangeListener {
                            previewSettings.opacityIdentifier = value as Double
                            refresh()
                        }
                    }
                }
            }
        }

        leftPanel.border = JBUI.Borders.empty(10)

        dialogPanel = leftPanel

        val scrollPanel = JBScrollPane(leftPanel)
        scrollPanel.border = JBUI.Borders.empty()

        myMainPanel.border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
        myMainPanel.add(scrollPanel, BorderLayout.WEST)
        myMainPanel.add(textField, BorderLayout.CENTER)
    }

    fun refresh() {
        textField.editor?.component?.repaint()
        val settings = Settings.KEY.getData(DataManager.getInstance().getDataContext(myMainPanel))
        // TODO: We can get the ID more directly once we move the Configurable to SearchableConfigurable.

        // There's normally a significant delay before the component detects changes. We must check Modified immediately
        // in order for the `Cancel()` callback to work reliably.
        settings?.checkModified("com.luxalpa.structuredhighlights.LxConfigurable")
    }

    fun getPanel(): JComponent = myMainPanel
}