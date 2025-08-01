package com.luxalpa.structuredhighlights

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import java.awt.Color

@Service
@State(
    name = "com.luxalpa.structuredhighlights.LxApplicationSettings",
    storages = [Storage("LxApplicationSettings.xml")]
)
class LxApplicationSettings :
    SerializablePersistentStateComponent<LxApplicationSettings.AppState>(AppState()) {
    var stringValue: String
        get() = state.stringValue
        set(value) {
            updateState {
                it.copy(stringValue = value)
            }
        }

    data class AppState(
        @JvmField var stringValue: String = "Hello World!"
//        @JvmField var previewColor: Color? = null,
//        @JvmField var colors: Map<BlockType, Color> = BlockType.entries.associateWith { it.toColor() }
    )

    companion object {
        val instance: LxApplicationSettings
            get() = ApplicationManager.getApplication().getService(
                LxApplicationSettings::class.java
            )
    }
}