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

    private val currentSettings: V2State
        get() = state.v2 ?: V2State()

    override fun loadState(state: AppState) {
        super.loadState(state)

        if (state.v2 == null) {
            val migrated = when {
                hasLegacyState(state) -> migrateLegacyToV2(state)
                else -> V2State()
            }

            updateState {
                it.copy(v2 = migrated)
            }
        }
    }

    private fun migrateLegacyToV2(state: AppState): V2State {
        // -- The colors now live in the color scheme instead.
        val legacyColors =
            state.highlightColors.orEmpty() + state.colors.orEmpty()

        if (legacyColors.isNotEmpty()) {
            debug { "Migrating to scheme" }

            legacyColors.forEach { (blockType, serializedColor) ->
                scheme.setColor(COLOR_KEYS.getValue(blockType), serializedColor.c)
            }
        }

        // The default values here are from the OLD version, because the serializer skips the defaults.
        val opacityNormal = state.opacityNormal ?: 0.035

        return V2State(
            opacityNormal,
            opacityHeader = 1f - (1f - (state.opacityHeader ?: 0.1)) * (1f - opacityNormal),
            opacitySubheader = 1f - (1f - (state.opacitySubheader ?: 0.06)) * (1f - opacityNormal),
        )
    }

    private fun hasLegacyState(state: AppState): Boolean =
        !state.colors.isNullOrEmpty() ||
                !state.highlightColors.isNullOrEmpty() ||
                state.opacityNormal != null ||
                state.opacityHeader != null ||
                state.opacitySubheader != null

    private fun updateCurrentSettings(
        transform: (V2State) -> V2State
    ) {
        updateState { appState ->
            appState.copy(
                v2 = transform(appState.v2 ?: V2State())
            )
        }
    }

    var opacityNormal: Double
        get() = currentSettings.opacityNormal
        set(value) {
            updateCurrentSettings { it.copy(opacityNormal = value) }
        }

    var opacityHeader: Double
        get() = currentSettings.opacityHeader
        set(value) {
            updateCurrentSettings { it.copy(opacityHeader = value) }
        }

    var opacitySubheader: Double
        get() = currentSettings.opacitySubheader
        set(value) {
            updateCurrentSettings { it.copy(opacitySubheader = value) }
        }

    var opacityIdentifier: Double
        get() = currentSettings.opacityIdentifier
        set(value) {
            updateCurrentSettings { it.copy(opacityIdentifier = value) }
        }

    val previewSettings: PreviewSettings
        get() = myPreviewSettings ?: PreviewSettings(
            opacityNormal,
            opacityHeader,
            opacitySubheader,
            opacityIdentifier,
        ).also { myPreviewSettings = it }

    override fun getOpacity(kind: Kind): Double {
        return when (kind) {
            Kind.Block -> currentSettings.opacityNormal
            Kind.Header -> currentSettings.opacityHeader
            Kind.Subheader -> currentSettings.opacitySubheader
            Kind.Identifier -> currentSettings.opacityIdentifier
        }
    }

    data class AppState(
        // Retained after migration to support downgrading to an older plugin.
        // Null means that no settings from the legacy version were loaded.
        @JvmField var colors: Map<BlockType, SerializedColor>? = null,
        @JvmField var highlightColors: Map<BlockType, SerializedColor>? = null,
        @JvmField var opacityNormal: Double? = null,
        @JvmField var opacityHeader: Double? = null,
        @JvmField var opacitySubheader: Double? = null,

        @JvmField var v2: V2State? = null,
    )

    data class V2State(
        @JvmField var opacityNormal: Double = 0.01,
        @JvmField var opacityHeader: Double = 0.064,
        @JvmField var opacitySubheader: Double = 0.05,
        @JvmField var opacityIdentifier: Double = 0.105,
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
    var opacityIdentifier: Double,
) :
    AppSettings {
    override fun getOpacity(kind: Kind): Double {
        return when (kind) {
            Kind.Block -> opacityNormal
            Kind.Header -> opacityHeader
            Kind.Subheader -> opacitySubheader
            Kind.Identifier -> opacityIdentifier
        }
    }

    fun reset() {
        val settings = LxApplicationSettings.instance
        opacityNormal = settings.opacityNormal
        opacityHeader = settings.opacityHeader
        opacitySubheader = settings.opacitySubheader
        opacityIdentifier = settings.opacityIdentifier
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
        "LUX_SH_RUST_BG_${blockType.name}",
        blockType.defaultColor()
    )
}