package com.luxalpa.structuredhighlights

import com.intellij.application.options.colors.ColorAndFontDescription
import com.intellij.application.options.colors.ColorAndFontDescriptionPanel
import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.application.options.colors.ColorAndFontPanelFactory
import com.intellij.application.options.colors.ColorAndFontSettingsListener
import com.intellij.application.options.colors.NewColorAndFontPanel
import com.intellij.application.options.colors.OptionsPanelImpl
import com.intellij.application.options.colors.OptionsPanelImpl.ColorDescriptionPanel
import com.intellij.application.options.colors.PreviewPanel
import com.intellij.application.options.colors.SchemesPanel
import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorAndFontDescriptorsProvider
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.LanguageTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.EventDispatcher
import java.awt.Color
import java.awt.event.ActionEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.text.DefaultFormatter

class LxColorsPageFactory : ColorAndFontPanelFactory, ColorAndFontDescriptorsProvider {
    override fun createPanel(options: ColorAndFontOptions): NewColorAndFontPanel {
        val schemesPanel = SchemesPanel(options)

        debug { "Creating panel" }

        val descriptionPanel = CompositeColorDescriptionPanel()
        descriptionPanel.addDescriptionPanel(
            CaretRowDescriptionPanel(),
            Condition {
                val description = it as? ColorAndFontDescription ?: return@Condition false
                description.type == EditorColors.CARET_ROW_COLOR.externalName
            }
        )
        descriptionPanel.addDescriptionPanel(
            ColorAndFontDescriptionPanel(),
            Condition {
                val description = it as? ColorAndFontDescription ?: return@Condition false
                description.type != EditorColors.CARET_ROW_COLOR.externalName
            }
        )

        val optionsPanel =
            OptionsPanelImpl(
                options,
                schemesPanel,
                getDisplayName(),
                descriptionPanel,
            )

        val previewPanel = LxPreviewPanel(options)

        schemesPanel.addListener(object : ColorAndFontSettingsListener.Abstract() {
            override fun schemeChanged(source: Any) {
                debug { "Scheme changed" }
                previewPanel.setColorScheme(options.selectedScheme)
                optionsPanel.updateOptionsList()
            }
        })

        return NewColorAndFontPanel(
            schemesPanel,
            optionsPanel,
            previewPanel,
            getDisplayName(),
            null,
            null
        )
    }

    override fun getPanelDisplayName(): @NlsContexts.ConfigurableName String = getDisplayName()
    override fun getAttributeDescriptors(): Array<out AttributesDescriptor?> = emptyArray<AttributesDescriptor>()

    override fun getColorDescriptors(): Array<out ColorDescriptor?> {
        val pluginColors = LanguageSupport.EP.extensionList.flatMap { support ->
            support.blockTypes.map { blockType ->
                ColorDescriptor(
                    "Languages//${support.displayName}//${blockType.label}",
                    blockType.colorKey,
                    ColorDescriptor.Kind.BACKGROUND
                )
            }
        }

        return (
                pluginColors + ColorDescriptor(
                    "General//Caret Row",
                    EditorColors.CARET_ROW_COLOR,
                    ColorDescriptor.Kind.BACKGROUND
                )
                ).toTypedArray()
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = MyBundle.message("pluginName")
}

class LxPreviewPanel(options: ColorAndFontOptions) : PreviewPanel {
    var textField: LxLanguageTextField?
    private var currentSupport: LanguageSupport? = null

    init {
        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        currentSupport = LanguageSupport.EP.extensionList.firstOrNull()

        textField = LxLanguageTextField(
            project,
            currentSupport?.previewText ?: "No preview text available.",
            currentSupport?.languageId ?: "",
            options
        )
    }

    private fun supportFor(selected: EditorSchemeAttributeDescriptor): LanguageSupport? {
        val type = selected.type ?: return null
        return LanguageSupport.EP.extensionList.firstOrNull { support ->
            support.blockTypes.any { it.key == type || it.colorKey.externalName == type }
        }
    }

    override fun blinkSelectedHighlightType(selected: Any?) {
        val desc = selected as? EditorSchemeAttributeDescriptor ?: return
        val support = supportFor(desc) ?: return

        if (support === currentSupport) return
        currentSupport = support

        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        val language = Language.findLanguageByID(support.languageId) ?: return
        val fileType = language.associatedFileType ?: return
        val doc = LanguageTextField.createDocument(
            support.previewText, language, project, LanguageTextField.SimpleDocumentCreator()
        )

        textField!!.setNewDocumentAndFileType(fileType, doc)
    }

    override fun disposeUIResources() {
        debug { "Disposing UI resources" }
        textField = null
    }

    override fun getPanel(): JComponent = textField!!

    override fun updateView() {
        val editor = textField?.editor as? EditorEx ?: return
        editor.reinitSettings()
        textField!!.repaint()
    }

    override fun addListener(listener: ColorAndFontSettingsListener) {

    }

    fun setColorScheme(scheme: EditorColorsScheme) {
        val editor = textField!!.editor as? EditorEx? ?: return
        editor.colorsScheme = scheme
        debug { "Updating scheme" }
    }
}


internal class CaretRowDescriptionPanel :
    ColorDescriptionPanel {
    private val myDispatcher =
        EventDispatcher.create(ColorDescriptionPanel.Listener::class.java)

    private var myPanel: JPanel
    private var caretRowOpacitySpinner: JSpinner? = null
    private var caretRowOpacity: Int = 255

    init {
        val panel = panel {
            row("Caret Row Opacity (0-255):") {
                val spinnerCell = spinner(0..255, 1)

                val spinner = spinnerCell.component
                caretRowOpacitySpinner = spinner

                // instantly send events whenever the value is changed - don't wait for the user to unfocus.
                val editor = spinner.editor as? JSpinner.DefaultEditor
                val formatter = editor?.textField?.formatter as? DefaultFormatter
                formatter?.commitsOnValidEdit = true

                spinnerCell.component.addChangeListener {
                    caretRowOpacity =
                        (spinnerCell.component.value as Number).toInt()
                    myDispatcher.multicaster.onSettingsChanged(
                        // The ActionEvent is ignored by the actual listener, so it doesn't matter what we put here.
                        ActionEvent(spinner, ActionEvent.ACTION_PERFORMED, "caretRowOpacity")
                    )
                }
            }
        }

        myPanel = panel
    }

    override fun getPanel(): JComponent {
        return myPanel
    }

    override fun resetDefault() {
        debug { "resetDefault called" }
    }

    override fun reset(attrDescription: EditorSchemeAttributeDescriptor) {
        if (attrDescription !is ColorAndFontDescription) return

        caretRowOpacity = attrDescription.backgroundColor.alpha
        caretRowOpacitySpinner?.value = caretRowOpacity
    }

    override fun apply(attrDescription: EditorSchemeAttributeDescriptor, scheme: EditorColorsScheme?) {
        if (attrDescription !is ColorAndFontDescription) return

        val current = attrDescription.backgroundColor
            ?: scheme?.getColor(EditorColors.CARET_ROW_COLOR)
            ?: Color.BLACK

        attrDescription.isBackgroundChecked = true
        attrDescription.backgroundColor = Color(
            current.red, current.green, current.blue, caretRowOpacity
        )
        attrDescription.apply(scheme)
    }

    override fun addListener(listener: ColorDescriptionPanel.Listener) {
        myDispatcher.addListener(listener)
    }
}