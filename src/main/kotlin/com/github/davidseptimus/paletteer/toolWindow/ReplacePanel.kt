package com.github.davidseptimus.paletteer.toolWindow

import com.github.davidseptimus.paletteer.PaletteerBundle
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColorPicker
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import java.awt.Color
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.text.AbstractDocument

/**
 * Panel for replacing colors in the editor color scheme, using IntelliJ UI DSL.
 */
class ReplacePanel(private val project: Project) : JPanel() {

    private var originalColor: Color? = null
    private var replacementColor: Color? = null
    private var includeTextAttributes = true
    private var includeEditorColors = true

    private val colorReplacementService = ColorReplacementService()

    init {
        layout = java.awt.BorderLayout()
        add(createReplacePanel(), java.awt.BorderLayout.CENTER)
    }

    private fun createReplacePanel() = panel {
        row {
            label(PaletteerBundle.message("toolWindow.replace"))
                .applyToComponent { border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5) }
        }
        row(PaletteerBundle.message("toolWindow.replace.originalColor")) {
            createColorRow(
                { color -> originalColor = color },
                { originalColor }
            )
        }
        row(PaletteerBundle.message("toolWindow.replace.replacementColor")) {
            createColorRow(
                { color -> replacementColor = color },
                { replacementColor }
            )
        }
        row {
            checkBox(PaletteerBundle.message("toolWindow.replace.includeTextAttributes"))
                .applyToComponent {
                    isSelected = true
                    addActionListener { includeTextAttributes = isSelected }
                }
        }
        row {
            checkBox(PaletteerBundle.message("toolWindow.replace.includeEditorColors"))
                .applyToComponent {
                    isSelected = true
                    addActionListener { includeEditorColors = isSelected }
                }
        }
        row {
            button(PaletteerBundle.message("toolWindow.replace.button")) {
                performColorReplacement()
            }
        }
    }

    private fun Row.createColorRow(
        colorSetter: (Color) -> Unit,
        colorGetter: () -> Color?
    ) {
        val textDimension = java.awt.Dimension(120, 40)
        val buttonDimension = java.awt.Dimension(50, 40)
        val colorTextField = JTextField().apply {
            (document as? AbstractDocument)?.documentFilter = HexColorDocumentFilter()
            preferredSize = textDimension
            minimumSize = textDimension
            maximumSize = textDimension
        }
        val colorButtonComponent = JPanel()
        colorButtonComponent.background = colorGetter() ?: JBColor.LIGHT_GRAY
        colorButtonComponent.toolTipText = PaletteerBundle.message("toolWindow.replace.pickColor")
        colorButtonComponent.cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        colorButtonComponent.maximumSize = buttonDimension
        colorButtonComponent.minimumSize = buttonDimension
        colorButtonComponent.preferredSize = buttonDimension
        colorButtonComponent.border = javax.swing.BorderFactory.createLineBorder(JBColor.DARK_GRAY)
        colorButtonComponent.alignmentY = CENTER_ALIGNMENT
        colorButtonComponent.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                ColorPicker.showDialog(
                    this@ReplacePanel,
                    PaletteerBundle.message("toolWindow.replace.pickColor"),
                    colorGetter() ?: JBColor.LIGHT_GRAY,
                    true,
                    null,
                    false
                )?.let { selectedColor ->
                    colorSetter(selectedColor)
                    colorButtonComponent.background = selectedColor
                    colorButtonComponent.repaint()
                    val hex = String.format("%06X", (0xFFFFFF and selectedColor.rgb))
                    colorTextField.text = hex
                }
            }
        })

        cell(colorTextField)
        cell(colorButtonComponent)

        colorTextField.document.addDocumentListener(
            object : javax.swing.event.DocumentListener {
                override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateColor()
                override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateColor()
                override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateColor()
                private fun updateColor() {
                    try {
                        val text = colorTextField.text.trim()
                        if (text.length == 6) {
                            val color = Color.decode("#$text")
                            colorSetter(color)
                            colorButtonComponent.background = color
                            colorButtonComponent.repaint()
                        }
                    } catch (_: Exception) {
                        // Invalid color format
                    }
                }
            })
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
        colorReplacementService.forceReloadScheme(scheme)
        Messages.showInfoMessage(
            project,
            PaletteerBundle.message("toolWindow.replace.success", replaced),
            PaletteerBundle.message("toolWindow.replace.success.title")
        )
    }
}
