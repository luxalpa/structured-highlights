package com.luxalpa.structuredhighlights

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.Key
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import java.awt.Color

@Service
@State(
    name = "com.luxalpa.structuredhighlights.LxApplicationSettings",
    storages = [Storage("LxApplicationSettings.xml")]
)
class LxApplicationSettings :
    SerializablePersistentStateComponent<LxApplicationSettings.AppState>(AppState()), AppSettings {

    private var myPreviewSettings: PreviewSettings? = null

    private val scheme
        get() = EditorColorsManager.getInstance().globalScheme

    override fun loadState(state: AppState) {
        super.loadState(state)
        if (!state.migratedToScheme) {
            debug { "Migrating scheme" }
            BlockType.entries.forEach { blockType ->
                state.colors[blockType]?.c?.let {
                    setColor(blockType, it)
                }
                state.highlightColors[blockType]?.c?.let {
                    setColor(blockType, it)
                }
            }
            updateState {
                it.copy(migratedToScheme = true)
            }
        }
    }

    fun getColor(blockType: BlockType): Color {
        return scheme.getColor(COLOR_KEYS.getValue(blockType)) ?: blockType.defaultColor()
    }

    fun setColor(blockType: BlockType, color: Color) {
        scheme.setColor(COLOR_KEYS.getValue(blockType), color)
        // No explicit scheme change notification is required for persistence.
        // UI that depends on these colors should repaint as needed.
    }

    fun getHighlightColor(blockType: BlockType): Color {
        return scheme.getColor(HIGHLIGHT_COLOR_KEYS.getValue(blockType)) ?: blockType.defaultHighlightColor()
    }

    fun setHighlightColor(blockType: BlockType, color: Color) {
        scheme.setColor(HIGHLIGHT_COLOR_KEYS.getValue(blockType), color)
    }

    var opacityNormal: Double
        get() = state.opacityNormal
        set(value) {
            updateState { it.copy(opacityNormal = value) }
        }

    var opacityHeader: Double
        get() = state.opacityHeader
        set(value) {
            updateState { it.copy(opacityHeader = value) }
        }

    var opacitySubheader: Double
        get() = state.opacitySubheader
        set(value) {
            updateState { it.copy(opacitySubheader = value) }
        }

    val previewSettings: PreviewSettings
        get() = myPreviewSettings ?: PreviewSettings(
            opacityNormal,
            opacityHeader,
            opacitySubheader,
        ).also { myPreviewSettings = it }

    override fun getOpacity(kind: Kind): Double {
        return when (kind) {
            Kind.Block -> state.opacityNormal
            Kind.Header -> state.opacityHeader
            Kind.Subheader -> state.opacitySubheader
            Kind.Identifier -> state.opacityNormal
        }
    }

    data class AppState(
        // Kept for one-time migration; no longer the source of truth for colors.
        @JvmField var colors: Map<BlockType, SerializedColor> = BlockType.entries.associateWith {
            SerializedColor(it.defaultColor())
        },
        @JvmField var highlightColors: Map<BlockType, SerializedColor> = BlockType.entries.associateWith {
            SerializedColor(it.defaultHighlightColor())
        },
        @JvmField var opacityNormal: Double = 0.035,
        @JvmField var opacityHeader: Double = 0.1,
        @JvmField var opacitySubheader: Double = 0.06,
        @JvmField var migratedToScheme: Boolean = false
    )

    @Tag("color")
    data class SerializedColor(
        @get:Attribute("value", converter = ColorConverter::class)
        var c: Color
    ) {
        // Needed for some reason to serialize. Else it just crashes.
        @Suppress("unused")
        constructor() : this(Color.WHITE)
    }

    companion object {
        val instance: LxApplicationSettings
            get() = ApplicationManager.getApplication().getService(
                LxApplicationSettings::class.java
            )
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

    fun reset() {
        val settings = LxApplicationSettings.instance
        opacityNormal = settings.opacityNormal
        opacityHeader = settings.opacityHeader
        opacitySubheader = settings.opacitySubheader
    }
}

class ColorConverter : Converter<Color>() {
    override fun fromString(value: String): Color? {
        return value.toIntOrNull()?.let { Color(it) }
    }

    override fun toString(value: Color): String = value.rgb.toString()
}

val COLOR_KEYS: Map<BlockType, ColorKey> = BlockType.entries.associateWith { blockType ->
    ColorKey.createColorKey(
        "LUX_SH_BG_${blockType.name}",
        blockType.defaultColor()
    )
}

val HIGHLIGHT_COLOR_KEYS: Map<BlockType, ColorKey> = BlockType.entries.associateWith { blockType ->
    ColorKey.createColorKey(
        "LUX_SH_HL_${blockType.name}",
        blockType.defaultHighlightColor()
    )
}
