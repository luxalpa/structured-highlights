package com.luxalpa.structuredhighlights

import com.intellij.application.options.colors.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorAndFontDescriptorsProvider
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.ColorPanel
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBUI
import fleet.util.associateNotNull
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class LxColorsPageFactory : ColorAndFontPanelFactory, ColorAndFontDescriptorsProvider {
    override fun createPanel(options: ColorAndFontOptions): NewColorAndFontPanel {
        val schemesPanel = SchemesPanel(options)

        val previewSettings = PreviewSettings(
            LxApplicationSettings.instance.state.opacityNormal,
            LxApplicationSettings.instance.state.opacityHeader,
            LxApplicationSettings.instance.state.opacitySubheader,
        )

        val optionsPanel = LxOptionsPanel(options, schemesPanel)
        val previewPanel = LxPreviewPanel(previewSettings, options)

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
    }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

    return colorSelect
}

class LxPreviewPanel(previewSettings: PreviewSettings, options: ColorAndFontOptions) : PreviewPanel {
    val textField: LxLanguageTextField

    init {
        val openProjects = ProjectManager.getInstance().openProjects
        val project = if (openProjects.isNotEmpty()) openProjects[0] else ProjectManager.getInstance().defaultProject

        textField = LxLanguageTextField(project, PREVIEW_TEXT, previewSettings, options)
    }

    override fun blinkSelectedHighlightType(selected: Any?) {

    }

    override fun disposeUIResources() {
        // TODO: Do we need to do anything here?
    }

    override fun getPanel(): JComponent = textField

    override fun updateView() {
        textField.repaint()
    }

    override fun addListener(listener: ColorAndFontSettingsListener) {

    }

    fun setColorScheme(scheme: EditorColorsScheme) {
        val editor = textField.editor as EditorEx? ?: return
        editor.colorsScheme = scheme
        debug { "Updating scheme" }
    }

}

val LUX_PREVIEW_SETTINGS: Key<PreviewSettings> = Key.create("LUX_PREVIEW_SETTINGS")

interface AppSettings {
    fun getOpacity(kind: Kind): Double
}

class PreviewSettings(
    var opacityNormal: Double,
    var opacityHeader: Double,
    var opacitySubheader: Double,
) :
    AppSettings {
    override fun getOpacity(kind: Kind): Double {
        return when (kind) {
            Kind.Block, Kind.Identifier -> opacityNormal
            Kind.Header -> opacityHeader
            Kind.Subheader -> opacitySubheader
        }
    }
}

class LxLanguageTextField(
    project: Project,
    text: String,
    val previewSettings: PreviewSettings,
    val options: ColorAndFontOptions
) :
    LanguageTextField(Language.findLanguageByID("Rust")!!, project, text, false) {

    init {
        this.font = EditorFontType.PLAIN.globalFont
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.colorsScheme = options.selectedScheme
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
        editor.putUserData(LUX_PREVIEW_SETTINGS, previewSettings)
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

/*

class LxConfigurable : Configurable, Configurable.NoScroll, Configurable.NoMargin {
    lateinit var mySettingsComponent: AppSettingsComponent

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = MyBundle.message("pluginName")

    override fun createComponent(): JComponent {
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
        debug { "Applying settings" }
        mySettingsComponent.dialogPanel.apply()
    }

    override fun reset() {
        mySettingsComponent.dialogPanel.reset()
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

        debug { EditorColorsManager.getInstance().globalScheme.name }

        previewSettings =
            PreviewSettings(
                LxApplicationSettings.instance.getAllColors().toMutableMap(),
                LxApplicationSettings.instance.getAllHighlightColors().toMutableMap(),
                LxApplicationSettings.instance.state.opacityNormal,
                LxApplicationSettings.instance.state.opacityHeader,
                LxApplicationSettings.instance.state.opacitySubheader,
            )

        textField = LxLanguageTextField(project, PREVIEW_TEXT, previewSettings)

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
}*/