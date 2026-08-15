package com.luxalpa.structuredhighlights

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.ide.DataManager
import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.bindValue
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class LxLanguageTextField(
    project: Project,
    text: String,
    val options: ColorAndFontOptions? = null,
) :
    LanguageTextField(Language.findLanguageByID("Rust")!!, project, text, false) {

    init {
        this.font = EditorFontType.PLAIN.globalFont
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()

        options?.selectedScheme?.let { scheme ->
            editor.colorsScheme = scheme
        }
        editor.setHorizontalScrollbarVisible(true)
        editor.setVerticalScrollbarVisible(true)
        editor.settings.isLineNumbersShown = true
        editor.settings.isAutoCodeFoldingEnabled = true
        editor.settings.isFoldingOutlineShown = true
        editor.settings.isIndentGuidesShown = true
        editor.settings.isLineMarkerAreaShown = true
        editor.settings.isCaretRowShown = true
        editor.isOneLineMode = false
        editor.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 1, 1, 1))
        editor.putUserData(LUX_PREVIEW_SETTINGS, LxApplicationSettings.instance.previewSettings)
        return editor
    }
}

val PREVIEW_TEXT = """
            trait Terrible {
                fn breathe_fire(&self);
                fn devour(&self, num_people: usize);
            }

            #[derive(Clone, Debug)]
            struct Dragon {
                pub name: String,
                pub age: f32
            }

            impl Dragon {
                pub fn roar(&self) {
                    println!("Roar!!!");
                }
            }

            impl Terrible for Dragon {
                fn breathe_fire(&self) {
                    println!("Breathing fire!");
                    self.roar();
                }
                
                fn devour(&self, num_people: usize) {
                    println!("Devouring {} snacks", num_people);
                    self.roar();
                }
            }

            enum Weapon {
                Tail,
                Claws { num_talons: usize },
                Wings,
                Teeth(usize),
                Fire,
            }

            #[cfg(test)]
            mod tests {
                use super::*;

                fn test_dragon() {
                    let dragon = Dragon {
                        name: "Smaug".to_string(),
                        age: 7000.0,
                    };

                    dragon.breathe_fire();
                }
            }
        """.trimIndent()

class LxConfigurable : Configurable, Configurable.NoScroll, Configurable.NoMargin {
    var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = MyBundle.message("pluginName")

    override fun createComponent(): JComponent {
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

        textField = LxLanguageTextField(project, PREVIEW_TEXT)

        myMainPanel = JPanel(BorderLayout())

        val leftPanel = panel {
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