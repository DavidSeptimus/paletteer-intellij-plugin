package com.github.davidseptimus.paletteerintellijplugin.action

import com.github.davidseptimus.paletteerintellijplugin.PaletteerBundle
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.Timer
import javax.swing.SwingUtilities

class TextAttributeDialog(
    private val attributes: List<AttributeInfo>
) : DialogWrapper(true) {

    data class AttributeInfo(
        val type: String, // "Markup" or "Syntax"
        val text: String,
        val key: String?,
        val foreground: Color?,
        val background: Color?,
    )

    init {
        title = PaletteerBundle.message("lookup.dialog.title")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)
        }

        attributes.forEach { attr ->
            val attributePanel = createAttributePanel(attr)
            mainPanel.add(attributePanel)
            mainPanel.add(Box.createVerticalStrut(15))
        }

        return mainPanel
    }

    private fun createAttributePanel(attr: AttributeInfo): JPanel {
        val panel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                JBUI.Borders.empty(10)
            )
        }

        // Type label (Markup/Syntax)
        panel.add(JBLabel("[${attr.type}]").apply {
            font = font.deriveFont(font.style or java.awt.Font.BOLD)
        })
        panel.add(Box.createVerticalStrut(8))

        // Text
        panel.add(createLabelValueRow(
            PaletteerBundle.message("lookup.dialog.text"),
            "\"${attr.text}\""
        ))
        panel.add(Box.createVerticalStrut(5))

        // Key
        if (attr.key != null) {
            panel.add(createLabelValueRow(
                PaletteerBundle.message("lookup.dialog.key"),
                attr.key
            ))
            panel.add(Box.createVerticalStrut(5))
        }

        // Foreground with color preview
        panel.add(createColorRow(
            PaletteerBundle.message("lookup.dialog.foreground"),
            attr.foreground
        ))
        panel.add(Box.createVerticalStrut(5))

        // Background with color preview
        panel.add(createColorRow(
            PaletteerBundle.message("lookup.dialog.background"),
            attr.background
        ))
        panel.add(Box.createVerticalStrut(5))

        return panel
    }

    private fun createLabelValueRow(label: String, value: String): JPanel {
        return JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout(5, 0)
            add(JBLabel(label).apply {
                font = font.deriveFont(font.style or java.awt.Font.BOLD)
            }, BorderLayout.WEST)
            add(JBTextField(value).apply {
                isEditable = false
                border = null
                background = null
            }, BorderLayout.CENTER)
        }
    }

    private fun createColorRow(label: String, color: Color?): JPanel {
        return JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout(5, 0)

            add(JBLabel(label).apply {
                font = font.deriveFont(font.style or java.awt.Font.BOLD)
            }, BorderLayout.WEST)

            val colorPanel = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout(5, 0)
            }

            val colorHex = color?.let { String.format("#%06X", 0xFFFFFF and it.rgb) } ?: "default"
            colorPanel.add(JBTextField(colorHex).apply {
                isEditable = false
                border = null
                background = null
                columns = 10
            }, BorderLayout.WEST)

            if (color != null) {
                val preview = JPanel().apply {
                    background = color
                    preferredSize = Dimension(40, 20)
                    maximumSize = Dimension(40, 20)
                    minimumSize = Dimension(40, 20)
                    border = BorderFactory.createLineBorder(JBColor.border())
                }
                // Add mouse listener for clipboard copy
                preview.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        copyToClipboard(colorHex)
                        showCopiedMessage(preview, "Copied: $colorHex")
                    }
                })
                colorPanel.add(preview, BorderLayout.EAST)
            }

            add(colorPanel, BorderLayout.CENTER)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    }

    private fun showCopiedMessage(parent: JPanel, message: String) {
        val window = JWindow(SwingUtilities.getWindowAncestor(parent))
        val label = JLabel(message)
        label.border = JBUI.Borders.empty(5)
        window.add(label)
        window.pack()
        val location = parent.locationOnScreen
        window.setLocation(location.x, location.y - window.height - 5)
        window.isVisible = true
        Timer(1200) { window.isVisible = false; window.dispose() }.start()
    }
}