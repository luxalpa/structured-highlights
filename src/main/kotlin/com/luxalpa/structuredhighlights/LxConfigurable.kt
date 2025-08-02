package com.luxalpa.structuredhighlights

import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.ColorPanel
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.bindValue
import com.intellij.ui.dsl.builder.panel
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
            PreviewSettings(
                LxApplicationSettings.instance.state.colors.mapValues { it.value.c }.toMutableMap(),
                LxApplicationSettings.instance.state.highlightColors.mapValues { it.value.c }.toMutableMap(),
                LxApplicationSettings.instance.state.opacityNormal,
                LxApplicationSettings.instance.state.opacityHeader,
                LxApplicationSettings.instance.state.opacitySubheader,
            )

        textField = LxLanguageTextField(project, getPreviewText(), previewSettings)

        myMainPanel = JPanel(BorderLayout())

        val leftPanel = panel {
            group("Colors") {
                BlockType.entries.forEach { blockType ->
                    createRow(this, previewSettings, blockType)
                }
            }
            group("Opacity") {
                row("Normal:") {
                    spinner(0.0..1.0, 0.005).bindValue(
                        LxApplicationSettings.instance::opacityNormal
                    ).applyToComponent {
                        addChangeListener {
                            previewSettings.opacityNormal = value as Double
                            textField.editor?.component?.repaint()
                        }
                    }
                }
                row("Header:") {
                    spinner(0.0..1.0, 0.005).bindValue(
                        LxApplicationSettings.instance::opacityHeader
                    ).applyToComponent {
                        addChangeListener {
                            previewSettings.opacityHeader = value as Double
                            textField.editor?.component?.repaint()
                        }
                    }
                }
                row("Subheader:") {
                    spinner(0.0..1.0, 0.005).bindValue(
                        LxApplicationSettings.instance::opacitySubheader
                    ).applyToComponent {
                        addChangeListener {
                            previewSettings.opacitySubheader = value as Double
                            textField.editor?.component?.repaint()
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

fun createRow(panel: Panel, previewSettings: PreviewSettings, blockType: BlockType) {
    panel.row("${blockType.label()}:") {
        val colorSelect = ColorPanel()
        val highlightColorSelect = ColorPanel()

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

        highlightColorSelect.addActionListener { event ->
            highlightColorSelect.selectedColor?.let { color ->
                previewSettings.highlightColors[blockType] = color
            }
        }

        cell(highlightColorSelect).bind(
            componentGet = { comp -> comp.selectedColor ?: blockType.defaultHighlightColor() },
            componentSet = { comp, value -> comp.selectedColor = value },
            prop = MutableProperty(
                { LxApplicationSettings.instance.getHighlightColor(blockType) },
                { value -> LxApplicationSettings.instance.setHighlightColor(blockType, value) },
            ),
        )
    }
}

interface AppSettings {
    fun getColor(blockType: BlockType): Color
    fun getHighlightColor(blockType: BlockType): Color = getColor(blockType)
    fun getOpacity(kind: Kind): Double
}

class PreviewSettings(
    val colors: MutableMap<BlockType, Color>,
    val highlightColors: MutableMap<BlockType, Color>,
    var opacityNormal: Double,
    var opacityHeader: Double,
    var opacitySubheader: Double,
) :
    AppSettings {
    override fun getColor(blockType: BlockType): Color {
        return colors[blockType] ?: blockType.defaultColor()
    }

    override fun getHighlightColor(blockType: BlockType): Color {
        return highlightColors[blockType] ?: blockType.defaultHighlightColor()
    }

    override fun getOpacity(kind: Kind): Double {
        return when (kind) {
            Kind.Block, Kind.Identifier -> opacityNormal
            Kind.Header -> opacityHeader
            Kind.Subheader -> opacitySubheader
        }
    }
}

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