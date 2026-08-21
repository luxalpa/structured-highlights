// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
// This file is cloned from `com/intellij/application/options/colors/CompositeColorDescriptionPanel.java` and converted
// to Kotlin.
// The original class is marked as internal which prevents us from using it.

package com.luxalpa.structuredhighlights

import com.intellij.application.options.colors.OptionsPanelImpl.ColorDescriptionPanel
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor
import com.intellij.openapi.util.Condition
import java.awt.Container
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max

open class CompositeColorDescriptionPanel : JPanel(), ColorDescriptionPanel {
    protected val myDescriptionPanels: MutableList<ColorDescriptionPanel> = ArrayList<ColorDescriptionPanel>()
    protected val myConditions: MutableList<Condition<in EditorSchemeAttributeDescriptor?>?> =
        ArrayList<Condition<in EditorSchemeAttributeDescriptor?>?>()

    private val myListeners: MutableList<ColorDescriptionPanel.Listener> = ArrayList<ColorDescriptionPanel.Listener>()

    private var myActive: ColorDescriptionPanel? = null

    fun addDescriptionPanel(
        descriptionPanel: ColorDescriptionPanel,
        condition: Condition<in EditorSchemeAttributeDescriptor?>
    ) {
        myDescriptionPanels.add(descriptionPanel)
        myConditions.add(condition)

        for (listener in myListeners) {
            descriptionPanel.addListener(listener)
        }

        updatePreferredSize()
    }

    private fun updatePreferredSize() {
        val preferredSize = Dimension()
        for (panel in myDescriptionPanels) {
            val size = panel.getPanel().getPreferredSize()
            preferredSize.setSize(
                max(size.getWidth(), preferredSize.getWidth()),
                max(size.getHeight(), preferredSize.getHeight())
            )
        }
        setPreferredSize(preferredSize)
    }

    override fun getPanel(): JComponent {
        return this
    }

    override fun resetDefault() {
        if (myActive != null) {
            val locker = PaintLocker(this)
            try {
                setPreferredSize(getSize()) // froze [this] size
                remove(myActive!!.getPanel())
                myActive = null
            } finally {
                locker.release()
            }
        }
    }

    override fun reset(descriptor: EditorSchemeAttributeDescriptor) {
        val oldPanel = if (myActive == null) null else myActive!!.getPanel()
        myActive = getPanelForDescriptor(descriptor)
        val newPanel = if (myActive == null) null else myActive!!.getPanel()

        if (oldPanel !== newPanel) {
            val locker = PaintLocker(this)
            try {
                if (oldPanel != null) {
                    remove(oldPanel)
                }
                if (newPanel != null) {
                    setPreferredSize(null) // make [this] resizable
                    add(newPanel)
                }
            } finally {
                locker.release()
            }
        }
        if (myActive != null) {
            myActive!!.reset(descriptor)
        }
    }

    private fun getPanelForDescriptor(descriptor: EditorSchemeAttributeDescriptor): ColorDescriptionPanel? {
        for (i in myConditions.indices.reversed()) {
            val condition: Condition<in EditorSchemeAttributeDescriptor?> = myConditions.get(i)!!
            val panel: ColorDescriptionPanel? = myDescriptionPanels.get(i)
            if (condition.value(descriptor)) return panel
        }
        return null
    }


    override fun apply(descriptor: EditorSchemeAttributeDescriptor, scheme: EditorColorsScheme?) {
        if (myActive != null) {
            myActive!!.apply(descriptor, scheme)
        }
    }

    override fun addListener(listener: ColorDescriptionPanel.Listener) {
        for (panel in myDescriptionPanels) {
            panel.addListener(listener)
        }
        myListeners.add(listener)
    }

    private class PaintLocker(component: JComponent) {
        private val myPaintHolder: Container
        private val myPaintState: Boolean

        init {
            myPaintHolder = component.getParent()
            myPaintState = myPaintHolder.getIgnoreRepaint()
            myPaintHolder.setIgnoreRepaint(true)
        }

        fun release() {
            myPaintHolder.validate()
            myPaintHolder.setIgnoreRepaint(myPaintState)
            myPaintHolder.repaint()
        }
    }
}