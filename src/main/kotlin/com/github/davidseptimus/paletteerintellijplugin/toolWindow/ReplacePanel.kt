package com.github.davidseptimus.paletteerintellijplugin.toolWindow

import com.github.davidseptimus.paletteerintellijplugin.PaletteerBundle
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColorChooserService
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AbstractDocument

/**
 * Panel for replacing colors in the editor color scheme.
 */
class ReplacePanel(private val project: Project) : JBPanel<JBPanel<*>>() {

    private var originalColor: Color? = null
    private var replacementColor: Color? = null
    private var includeTextAttributes = true
    private var includeEditorColors = true

    private val colorReplacementService = ColorReplacementService()

    init {
        layout = BorderLayout()
        add(JBLabel(PaletteerBundle.message("toolWindow.replace")).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }, BorderLayout.NORTH)

        // Content area for replace functionality
        val contentPanel = createContentPanel()
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun createContentPanel(): JBPanel<JBPanel<*>> {
        return JBPanel<JBPanel<*>>().apply {
            layout = GridBagLayout()
            val gbc = GridBagConstraints().apply {
                insets = JBUI.insets(5)
                fill = GridBagConstraints.HORIZONTAL
            }

            // Original color row
            addColorRow(
                gbc,
                0,
                PaletteerBundle.message("toolWindow.replace.originalColor"),
                { color -> originalColor = color },
                { originalColor }
            )

            // Replacement color row
            addColorRow(
                gbc,
                1,
                PaletteerBundle.message("toolWindow.replace.replacementColor"),
                { color -> replacementColor = color },
                { replacementColor }
            )

            // Text attributes checkbox
            gbc.gridx = 0
            gbc.gridy = 2
            gbc.gridwidth = 4
            gbc.weightx = 1.0
            gbc.fill = GridBagConstraints.HORIZONTAL
            val textAttributesCheckbox = JBCheckBox(
                PaletteerBundle.message("toolWindow.replace.includeTextAttributes"),
                true
            ).apply {
                addActionListener {
                    includeTextAttributes = isSelected
                }
            }
            add(textAttributesCheckbox, gbc)

            // Editor colors checkbox
            gbc.gridy = 3
            val editorColorsCheckbox = JBCheckBox(
                PaletteerBundle.message("toolWindow.replace.includeEditorColors"),
                true
            ).apply {
                addActionListener {
                    includeEditorColors = isSelected
                }
            }
            add(editorColorsCheckbox, gbc)

            // Replace button
            gbc.gridy = 4
            val replaceButton = JButton(PaletteerBundle.message("toolWindow.replace.button")).apply {
                addActionListener {
                    performColorReplacement()
                }
            }
            add(replaceButton, gbc)
        }
    }

    private fun JBPanel<JBPanel<*>>.addColorRow(
        gbc: GridBagConstraints,
        row: Int,
        labelText: String,
        colorSetter: (Color) -> Unit,
        colorGetter: () -> Color?
    ) {
        // Label
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        add(JBLabel(labelText), gbc)

        // Text field
        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        val colorTextField = JBTextField().apply {
            (document as? AbstractDocument)?.documentFilter = HexColorDocumentFilter()
        }
        add(colorTextField, gbc)

        // Color picker button
        gbc.gridx = 2
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        val colorButton = JButton(PaletteerBundle.message("toolWindow.replace.pickColor"))
        add(colorButton, gbc)

        // Color display panel
        gbc.gridx = 3
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.BOTH
        val colorDisplay = JBPanel<JBPanel<*>>().apply {
            preferredSize = Dimension(50, 25)
            minimumSize = Dimension(50, 25)
            background = JBColor.WHITE
            border = BorderFactory.createLineBorder(JBColor.GRAY)
        }
        add(colorDisplay, gbc)

        // Update display from text field
        colorTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateColor()
            override fun removeUpdate(e: DocumentEvent?) = updateColor()
            override fun changedUpdate(e: DocumentEvent?) = updateColor()

            private fun updateColor() {
                try {
                    val text = colorTextField.text.trim()
                    if (text.length == 6) {
                        val color = Color.decode("#$text")
                        colorSetter(color)
                        colorDisplay.background = color
                        colorDisplay.repaint()
                    }
                } catch (_: Exception) {
                    // Invalid color format
                }
            }
        })

        // Color picker button action
        colorButton.addActionListener {
            val color = ColorChooserService.instance.showDialog(
                project,
                this,
                PaletteerBundle.message("toolWindow.replace.selectColor"),
                colorGetter(),
                true
            )
            if (color != null) {
                colorSetter(color)
                colorDisplay.background = color
                colorTextField.text = String.format("%06X", color.rgb and 0xFFFFFF)
            }
        }
    }

    private fun performColorReplacement() {
        val original = originalColor
        val replacement = replacementColor

        if (original == null || replacement == null) {
            Messages.showWarningDialog(
                project,
                PaletteerBundle.message("toolWindow.replace.error.selectColors"),
                PaletteerBundle.message("toolWindow.replace.error.title")
            )
            return
        }

        val scheme = EditorColorsManager.getInstance().globalScheme
        val replaced = colorReplacementService.replaceColorsInScheme(
            scheme,
            original,
            replacement,
            includeTextAttributes,
            includeEditorColors
        )

        Messages.showInfoMessage(
            project,
            PaletteerBundle.message("toolWindow.replace.success", replaced),
            PaletteerBundle.message("toolWindow.replace.success.title")
        )
    }
}

