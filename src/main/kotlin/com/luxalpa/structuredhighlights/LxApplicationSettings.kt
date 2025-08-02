package com.luxalpa.structuredhighlights

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
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

    override fun getColor(blockType: BlockType): Color = state.colors[blockType]?.c ?: blockType.defaultColor()
    fun setColor(blockType: BlockType, color: Color) {
        updateState {
            it.copy(colors = it.colors + (blockType to SerializedColor(color)))
        }
    }

    override fun getHighlightColor(blockType: BlockType): Color =
        state.highlightColors[blockType]?.c ?: blockType.defaultHighlightColor()

    fun setHighlightColor(blockType: BlockType, color: Color) {
        updateState {
            it.copy(highlightColors = it.highlightColors + (blockType to SerializedColor(color)))
        }
    }

    var opacityNormal: Double
        get() = state.opacityNormal
        set(value) {
            updateState {
                it.copy(opacityNormal = value)
            }
        }

    var opacityHeader: Double
        get() = state.opacityHeader
        set(value) {
            updateState {
                it.copy(opacityHeader = value)
            }
        }

    var opacitySubheader: Double
        get() = state.opacitySubheader
        set(value) {
            updateState {
                it.copy(opacitySubheader = value)
            }
        }

    override fun getOpacity(kind: Kind): Double {
        return when (kind) {
            Kind.Block -> state.opacityNormal
            Kind.Header -> state.opacityHeader
            Kind.Subheader -> state.opacitySubheader
            Kind.Identifier -> state.opacityNormal
        }
    }

    data class AppState(
        @JvmField var colors: Map<BlockType, SerializedColor> = BlockType.entries.associateWith {
            SerializedColor(it.defaultColor())
        },

        @JvmField var highlightColors: Map<BlockType, SerializedColor> = BlockType.entries.associateWith {
            SerializedColor(it.defaultHighlightColor())
        },

        @JvmField var opacityNormal: Double = 0.035,
        @JvmField var opacityHeader: Double = 0.1,
        @JvmField var opacitySubheader: Double = 0.06
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

class ColorConverter : Converter<Color>() {
    override fun fromString(value: String): Color? {
        return value.toIntOrNull()?.let { Color(it) }
    }

    override fun toString(value: Color): String? = value.rgb.toString()
}