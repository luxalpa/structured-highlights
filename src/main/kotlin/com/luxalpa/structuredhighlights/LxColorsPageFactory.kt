package com.luxalpa.structuredhighlights

import com.intellij.application.options.colors.ColorAndFontDescription
import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.application.options.colors.ColorAndFontPanelFactory
import com.intellij.application.options.colors.ColorAndFontSettingsListener
import com.intellij.application.options.colors.NewColorAndFontPanel
import com.intellij.application.options.colors.OptionsPanel
import com.intellij.application.options.colors.PreviewPanel
import com.intellij.application.options.colors.SchemesPanel
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorAndFontDescriptorsProvider
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class LxColorsPageFactory : ColorAndFontPanelFactory, ColorAndFontDescriptorsProvider {
    override fun createPanel(options: ColorAndFontOptions): NewColorAndFontPanel {
        val schemesPanel = SchemesPanel(options)

        debug { "Creating panel" }

        val optionsPanel = LxOptionsPanel(options, schemesPanel)
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
            MyBundle.message("pluginName"),
            null,
            null
        )
    }

    override fun getPanelDisplayName(): @NlsContexts.ConfigurableName String = MyBundle.message("pluginName")
    override fun getAttributeDescriptors(): Array<out AttributesDescriptor?> = emptyArray<AttributesDescriptor>()

    override fun getColorDescriptors(): Array<out ColorDescriptor?> {
        return COLOR_KEYS.entries
            .map { ColorDescriptor(it.key.label(), it.value, ColorDescriptor.Kind.BACKGROUND) }
            .toTypedArray()
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = MyBundle.message("pluginName")
}

class LxOptionsPanel(val myOptions: ColorAndFontOptions, val mySchemesPanel: SchemesPanel) : OptionsPanel {
    val mainPanel: JPanel = JPanel(BorderLayout())
    var myDescriptors: Map<ColorKey, ColorAndFontDescription> = emptyMap()
    val myColorPanels: Map<ColorKey, ColorPanel>
    val myCategoryName = MyBundle.message("pluginName")

    private val myDispatcher =
        EventDispatcher.create(ColorAndFontSettingsListener::class.java)

    init {

        var theColorPanels: Map<ColorKey, ColorPanel>? = null
        val innerPanel = panel {
            theColorPanels = BlockType.entries.associate { blockType ->
                COLOR_KEYS.getValue(blockType) to createRow(this, blockType, myOptions, myDispatcher)
            }
        }

        innerPanel.border = JBUI.Borders.empty(0, 10)

        val scrollPanel = JBScrollPane(innerPanel)

        myColorPanels = theColorPanels!!

        mainPanel.add(scrollPanel, BorderLayout.CENTER)

        myOptions.addListener(object : ColorAndFontSettingsListener.Abstract() {
            override fun settingsChanged() {
                if (!mySchemesPanel.areSchemesLoaded()) return
                processListValueChanged()
            }
        })
    }

    override fun addListener(listener: ColorAndFontSettingsListener) {
        myDispatcher.addListener(listener)
    }

    override fun getPanel(): JPanel = mainPanel

    /// Whenever the scheme changes, we need to fetch the ColorDescriptors again and then update the ColorPanel.
    override fun updateOptionsList() {
        myDescriptors = myOptions.currentDescriptions.asSequence()
            .filter { description -> description.group == myCategoryName }
            .mapNotNull { it as? ColorAndFontDescription }
            .associateBy { description -> ColorKey.find(description.type) }

        processListValueChanged()
    }

    // When the scheme changes somehow, then we need to update the controls to match the data from the scheme.
    fun processListValueChanged() {
        for (descriptor in myDescriptors) {
            myColorPanels[descriptor.key]?.selectedColor = descriptor.value.backgroundColor
        }
    }

    override fun showOption(option: String?): Runnable? = null


    override fun applyChangesToScheme() {
        for (descriptor in myDescriptors) {
            descriptor.value.backgroundColor = myColorPanels[descriptor.key]?.selectedColor
            descriptor.value.apply(myOptions.selectedScheme)
        }
    }

    override fun selectOption(typeToSelect: String?) {

    }

    /// Not entirely sure what this is needed for - maybe for the search.
    override fun processListOptions(): Set<String> {
        return myOptions.currentDescriptions
            .asSequence()
            .filter { description -> description.group == myCategoryName }
            .map { it.toString() }.toSet()
    }

}

fun createRow(
    panel: Panel,
    blockType: BlockType,
    options: ColorAndFontOptions,
    myDispatcher: EventDispatcher<ColorAndFontSettingsListener>
): ColorPanel {
    val colorSelect = ColorPanel()
//        val highlightColorSelect = ColorPanel()

    panel.row("${blockType.label()}:") {

        colorSelect.addActionListener { event ->
            colorSelect.selectedColor?.let { color ->
                // This will trigger NewColorAndFontPanel, which then runs `applyChangesToScheme` on the OptionsPanel,
                // and then `updateView` on the PreviewPanel.
                myDispatcher.multicaster.settingsChanged()
                options.stateChanged()
            }
        }

        cell(colorSelect)

//        highlightColorSelect.addActionListener { event ->
//            highlightColorSelect.selectedColor?.let { color ->
//                previewSettings.highlightColors[blockType] = color
//            }
//        }
//
//        cell(highlightColorSelect)
    }

    return colorSelect
}

class LxPreviewPanel(options: ColorAndFontOptions) : PreviewPanel {
    var textField: LxLanguageTextField?

    init {
        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        textField = LxLanguageTextField(project, PREVIEW_TEXT, options)
    }

    override fun blinkSelectedHighlightType(selected: Any?) {

    }

    override fun disposeUIResources() {
        debug { "Disposing UI resources" }
        textField = null
    }

    override fun getPanel(): JComponent = textField!!

    override fun updateView() {
        textField!!.repaint()
    }

    override fun addListener(listener: ColorAndFontSettingsListener) {

    }

    fun setColorScheme(scheme: EditorColorsScheme) {
        val editor = textField!!.editor as EditorEx? ?: return
        editor.colorsScheme = scheme
        debug { "Updating scheme" }
    }

}