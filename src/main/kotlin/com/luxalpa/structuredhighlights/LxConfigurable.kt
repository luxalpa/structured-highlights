package com.luxalpa.structuredhighlights

import com.github.weisj.jsvg.cs
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.eel.provider.utils.copy
import com.intellij.ui.ColorPanel
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JPanel

class LxConfigurable : Configurable, Configurable.NoScroll, Configurable.NoMargin {
    lateinit var mySettingsComponent: AppSettingsComponent

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? = MyBundle.message("pluginName")

    override fun createComponent(): JComponent? {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent.getPanel()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun isModified(): Boolean {
        return mySettingsComponent.dialogPanel.isModified()
    }

    override fun apply() {
        mySettingsComponent.dialogPanel.apply()
    }

    override fun reset() {
        mySettingsComponent.dialogPanel.reset()
    }
}

val LUX_PREVIEW_SETTINGS: Key<PreviewSettings> = Key.create("LUX_PREVIEW_SETTINGS")

class AppSettingsComponent {
    val myMainPanel: JPanel
    val textField: LxLanguageTextField
    val dialogPanel: DialogPanel

    init {
        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        val previewSettings =
            PreviewSettings(LxApplicationSettings.instance.state.colors.mapValues { it.value.c }.toMutableMap())

        textField = LxLanguageTextField(project, getPreviewText(), previewSettings)

        myMainPanel = JPanel(BorderLayout())

        val leftPanel = panel {
            BlockType.entries.forEach { blockType ->
                createRow(this, previewSettings, blockType, textField)
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

    fun getPanel(): JComponent = myMainPanel

    fun getPreviewText(): String = """
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
}

fun createRow(panel: Panel, previewSettings: PreviewSettings, blockType: BlockType, textField: LxLanguageTextField) {
    panel.row("${blockType.label()}:") {
        val colorSelect = ColorPanel()

        colorSelect.addActionListener { event ->
            colorSelect.selectedColor?.let { color ->
                previewSettings.colors[blockType] = color
            }
        }

        cell(colorSelect).bind(
            componentGet = { comp -> comp.selectedColor ?: blockType.defaultColor() },
            componentSet = { comp, value -> comp.selectedColor = value },
            prop = MutableProperty(
                { LxApplicationSettings.instance.getColor(blockType) },
                { value -> LxApplicationSettings.instance.setColor(blockType, value) },
            ),
        )
    }
}

data class PreviewSettings(
    var colors: MutableMap<BlockType, Color>
)

class LxLanguageTextField(project: Project, text: String, val previewSettings: PreviewSettings) :
    LanguageTextField(Language.findLanguageByID("Rust")!!, project, text, false) {

    init {
        this.font = EditorFontType.PLAIN.globalFont
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setHorizontalScrollbarVisible(true)
        editor.setVerticalScrollbarVisible(true)
        editor.settings.isLineNumbersShown = true
        editor.settings.isAutoCodeFoldingEnabled = true
        editor.settings.isFoldingOutlineShown = true
        editor.settings.isIndentGuidesShown = true
        editor.settings.isLineMarkerAreaShown = true
        editor.settings.isCaretRowShown = true
        editor.isOneLineMode = false
        editor.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 1, 0, 1))
        editor.putUserData(LUX_PREVIEW_SETTINGS, previewSettings)
        return editor
    }
}