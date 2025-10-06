package com.github.davidseptimus.paletteerintellijplugin.toolWindow

import com.github.davidseptimus.paletteerintellijplugin.PaletteerBundle
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColorChooserService
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter
import kotlin.text.Regex


class PaletteerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val paletteerToolWindow = PaletteerToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(paletteerToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class PaletteerToolWindow(private val toolWindow: ToolWindow) {

        private var originalColor: Color? = null
        private var replacementColor: Color? = null
        private var includeTextAttributes = true
        private var includeEditorColors = true

        private var useRegex = false
        private var lookupTextAttributes = true
        private val searchHistory = mutableListOf<String>()
        private var historyIndex = -1

        private class HexColorDocumentFilter : DocumentFilter() {
            override fun insertString(fb: FilterBypass, offset: Int, string: String, attr: AttributeSet?) {
                val filtered = filterInput(string)
                if (filtered.isNotEmpty()) {
                    super.insertString(fb, offset, filtered, attr)
                }
            }

            override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
                val filtered = filterInput(text)
                super.replace(fb, offset, length, filtered, attrs)
            }

            private fun filterInput(string: String): String {
                // Allow only hex characters (0-9, A-F, a-f), strip out # and everything else
                return string.filter { char ->
                    char.isDigit() || char.uppercaseChar() in 'A'..'F'
                }
            }
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()

            val splitter = JBSplitter(true, 0.5f).apply {
                // Lookup section (top)
                firstComponent = createLookupPanel()

                // Replace section (bottom)
                secondComponent = createReplacePanel()
            }

            add(splitter, BorderLayout.CENTER)
        }

        private fun createLookupPanel() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            add(JBLabel(PaletteerBundle.message("toolWindow.lookup")).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
            }, BorderLayout.NORTH)

            val searchField = JBTextField()

            // Controls panel
            val controlsPanel = JBPanel<JBPanel<*>>().apply {
                layout = GridBagLayout()
                val gbc = GridBagConstraints().apply {
                    insets = JBUI.insets(5)
                    fill = GridBagConstraints.HORIZONTAL
                }

                // Search field
                gbc.gridx = 0
                gbc.gridy = 0
                gbc.weightx = 1.0
                add(searchField, gbc)

                // Options panel (regex checkbox and radio buttons)
                gbc.gridy = 1
                gbc.weightx = 1.0
                val optionsPanel = JBPanel<JBPanel<*>>().apply {
                    layout = FlowLayout(FlowLayout.LEFT)

                    val regexCheckbox = JBCheckBox(PaletteerBundle.message("toolWindow.lookup.useRegex"), false).apply {
                        addActionListener {
                            useRegex = isSelected
                        }
                    }
                    add(regexCheckbox)

                    val buttonGroup = ButtonGroup()

                    val textAttributesRadio = JBRadioButton(
                        PaletteerBundle.message("toolWindow.lookup.textAttributes"),
                        true
                    ).apply {
                        addActionListener {
                            lookupTextAttributes = true
                        }
                    }
                    buttonGroup.add(textAttributesRadio)
                    add(textAttributesRadio)

                    val editorColorsRadio = JBRadioButton(
                        PaletteerBundle.message("toolWindow.lookup.editorColors"),
                        false
                    ).apply {
                        addActionListener {
                            lookupTextAttributes = false
                        }
                    }
                    buttonGroup.add(editorColorsRadio)
                    add(editorColorsRadio)
                }
                add(optionsPanel, gbc)
            }
            add(controlsPanel, BorderLayout.NORTH)

            // Results table
            val tableModel = object : DefaultTableModel() {
                override fun isCellEditable(row: Int, column: Int) = false
            }
            tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.key"))
            tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.foreground"))
            tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.background"))
            tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.effects"))
            tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.bold"))
            tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.italic"))

            val resultsTable = JBTable(tableModel).apply {
                // Use transparent selection background
                val transparentSelection = Color(
                    JBColor.BLUE.red,
                    JBColor.BLUE.green,
                    JBColor.BLUE.blue,
                    30 // Alpha value for transparency
                )
                selectionBackground = transparentSelection
                selectionForeground = foreground

                setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
                    override fun getTableCellRendererComponent(
                        table: JTable?,
                        value: Any?,
                        isSelected: Boolean,
                        hasFocus: Boolean,
                        row: Int,
                        column: Int
                    ): Component {
                        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

                        // Color columns (1 and 2) show color swatches
                        if (column == 1 || column == 2) {
                            if (value is Color) {
                                background = value
                                text = String.format("%06X", value.rgb and 0xFFFFFF)
                            } else {
                                background = table?.background
                                text = ""
                            }
                        } else {
                            // For non-color columns, use transparent selection
                            background = if (isSelected) transparentSelection else table?.background
                            foreground = table?.foreground
                        }

                        // Add border for selected cells
                        border = if (isSelected) {
                            BorderFactory.createLineBorder(JBColor.BLUE, 1)
                        } else {
                            BorderFactory.createEmptyBorder(1, 1, 1, 1)
                        }

                        return component
                    }
                })

                // Use checkbox renderer for Bold and Italic columns
                val checkboxRenderer = object : DefaultTableCellRenderer() {
                    private val checkbox = JBCheckBox()

                    override fun getTableCellRendererComponent(
                        table: JTable?,
                        value: Any?,
                        isSelected: Boolean,
                        hasFocus: Boolean,
                        row: Int,
                        column: Int
                    ): Component {
                        checkbox.isSelected = value as? Boolean ?: false
                        checkbox.isEnabled = false
                        checkbox.background = if (isSelected) transparentSelection else table?.background
                        checkbox.horizontalAlignment = javax.swing.SwingConstants.CENTER
                        checkbox.border = if (isSelected) {
                            BorderFactory.createLineBorder(JBColor.BLUE, 1)
                        } else {
                            BorderFactory.createEmptyBorder(1, 1, 1, 1)
                        }
                        return checkbox
                    }
                }

                columnModel.getColumn(4).cellRenderer = checkboxRenderer
                columnModel.getColumn(5).cellRenderer = checkboxRenderer
            }

            val scrollPane = JBScrollPane(resultsTable)
            add(scrollPane, BorderLayout.CENTER)

            // Search history navigation with arrow keys
            searchField.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    when (e.keyCode) {
                        KeyEvent.VK_UP -> {
                            if (searchHistory.isNotEmpty()) {
                                if (historyIndex == -1) {
                                    historyIndex = searchHistory.size - 1
                                } else if (historyIndex > 0) {
                                    historyIndex--
                                }
                                searchField.text = searchHistory[historyIndex]
                                e.consume()
                            }
                        }
                        KeyEvent.VK_DOWN -> {
                            if (searchHistory.isNotEmpty() && historyIndex != -1) {
                                if (historyIndex < searchHistory.size - 1) {
                                    historyIndex++
                                    searchField.text = searchHistory[historyIndex]
                                } else {
                                    historyIndex = -1
                                    searchField.text = ""
                                }
                                e.consume()
                            }
                        }
                        KeyEvent.VK_ENTER -> {
                            val query = searchField.text.trim()
                            if (query.isNotEmpty() && (searchHistory.isEmpty() || searchHistory.last() != query)) {
                                searchHistory.add(query)
                                historyIndex = -1
                            }
                        }
                    }
                }
            })

            // Search on text change
            searchField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = performSearch(searchField.text, tableModel)
                override fun removeUpdate(e: DocumentEvent?) = performSearch(searchField.text, tableModel)
                override fun changedUpdate(e: DocumentEvent?) = performSearch(searchField.text, tableModel)
            })

            // Initial search to populate table
            performSearch("", tableModel)
        }

        private fun performSearch(query: String, tableModel: DefaultTableModel) {
            tableModel.rowCount = 0

            val scheme = EditorColorsManager.getInstance().globalScheme

            if (lookupTextAttributes) {
                // Search text attributes
                val attributeKeys = TextAttributesKey.getAllKeys()
                attributeKeys.forEach { key ->
                    val keyName = key.externalName
                    if (matchesSearch(keyName, query)) {
                        val attrs = scheme.getAttributes(key)
                        if (attrs != null) {
                            tableModel.addRow(arrayOf(
                                keyName,
                                attrs.foregroundColor,
                                attrs.backgroundColor,
                                attrs.effectType?.toString() ?: "",
                                attrs.fontType and 1 != 0,
                                attrs.fontType and 2 != 0
                            ))
                        }
                    }
                }
            } else {
                // Search editor colors
                val colorKeys = (scheme as? com.intellij.openapi.editor.colors.impl.AbstractColorsScheme)?.colorKeys
                colorKeys?.forEach { key ->
                    val keyName = key.externalName
                    if (matchesSearch(keyName, query)) {
                        val color = scheme.getColor(key)
                        tableModel.addRow(arrayOf(
                            keyName,
                            color,
                            null,
                            "",
                            false,
                            false
                        ))
                    }
                }
            }
        }

        private fun matchesSearch(text: String, query: String): Boolean {
            if (query.isBlank()) return true
            return if (useRegex) {
                try {
                    Regex(query).containsMatchIn(text)
                } catch (_: Exception) {
                    false
                }
            } else {
                text.contains(query, ignoreCase = true)
            }
        }

        private fun createReplacePanel() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            add(JBLabel(PaletteerBundle.message("toolWindow.replace")).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
            }, BorderLayout.NORTH)

            // Content area for replace functionality
            val contentPanel = JBPanel<JBPanel<*>>().apply {
                layout = GridBagLayout()
                val gbc = GridBagConstraints().apply {
                    insets = JBUI.insets(5)
                    fill = GridBagConstraints.HORIZONTAL
                }

                // Original color label
                gbc.gridx = 0
                gbc.gridy = 0
                gbc.weightx = 0.0
                add(JBLabel(PaletteerBundle.message("toolWindow.replace.originalColor")), gbc)

                // Original color text field
                gbc.gridx = 1
                gbc.weightx = 1.0
                gbc.fill = GridBagConstraints.HORIZONTAL
                val originalColorTextField = JBTextField().apply {
                    (document as? AbstractDocument)?.documentFilter = HexColorDocumentFilter()
                }
                add(originalColorTextField, gbc)

                // Original color picker button
                gbc.gridx = 2
                gbc.weightx = 0.0
                gbc.fill = GridBagConstraints.NONE
                val originalColorButton = JButton(PaletteerBundle.message("toolWindow.replace.pickColor"))
                add(originalColorButton, gbc)

                // Original color display panel
                gbc.gridx = 3
                gbc.weightx = 0.0
                gbc.fill = GridBagConstraints.BOTH
                val originalColorDisplay = JBPanel<JBPanel<*>>().apply {
                    preferredSize = Dimension(50, 25)
                    minimumSize = Dimension(50, 25)
                    background = Color.WHITE
                    border = javax.swing.BorderFactory.createLineBorder(Color.GRAY)
                }
                add(originalColorDisplay, gbc)

                // Update display from text field
                originalColorTextField.document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent?) = updateColor()
                    override fun removeUpdate(e: DocumentEvent?) = updateColor()
                    override fun changedUpdate(e: DocumentEvent?) = updateColor()

                    private fun updateColor() {
                        try {
                            val text = originalColorTextField.text.trim()
                            if (text.length == 6) {
                                val color = Color.decode("#$text")
                                originalColor = color
                                originalColorDisplay.background = color
                                originalColorDisplay.repaint()
                            }
                        } catch (_: Exception) {
                            // Invalid color format
                        }
                    }
                })

                originalColorButton.addActionListener {
                    val color = ColorChooserService.instance.showDialog(
                        toolWindow.project,
                        this@apply,
                        PaletteerBundle.message("toolWindow.replace.selectColor"),
                        originalColor,
                        true
                    )
                    if (color != null) {
                        originalColor = color
                        originalColorDisplay.background = color
                        originalColorTextField.text = String.format("%06X", color.rgb and 0xFFFFFF)
                    }
                }

                // Replacement color label
                gbc.gridx = 0
                gbc.gridy = 1
                gbc.weightx = 0.0
                gbc.fill = GridBagConstraints.HORIZONTAL
                add(JBLabel(PaletteerBundle.message("toolWindow.replace.replacementColor")), gbc)

                // Replacement color text field
                gbc.gridx = 1
                gbc.weightx = 1.0
                gbc.fill = GridBagConstraints.HORIZONTAL
                val replacementColorTextField = JBTextField().apply {
                    (document as? AbstractDocument)?.documentFilter = HexColorDocumentFilter()
                }
                add(replacementColorTextField, gbc)

                // Replacement color picker button
                gbc.gridx = 2
                gbc.weightx = 0.0
                gbc.fill = GridBagConstraints.NONE
                val replacementColorButton = JButton(PaletteerBundle.message("toolWindow.replace.pickColor"))
                add(replacementColorButton, gbc)

                // Replacement color display panel
                gbc.gridx = 3
                gbc.weightx = 0.0
                gbc.fill = GridBagConstraints.BOTH
                val replacementColorDisplay = JBPanel<JBPanel<*>>().apply {
                    preferredSize = Dimension(50, 25)
                    minimumSize = Dimension(50, 25)
                    background = Color.WHITE
                    border = javax.swing.BorderFactory.createLineBorder(Color.GRAY)
                }
                add(replacementColorDisplay, gbc)

                // Update display from text field
                replacementColorTextField.document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent?) = updateColor()
                    override fun removeUpdate(e: DocumentEvent?) = updateColor()
                    override fun changedUpdate(e: DocumentEvent?) = updateColor()

                    private fun updateColor() {
                        try {
                            val text = replacementColorTextField.text.trim()
                            if (text.length == 6) {
                                val color = Color.decode("#$text")
                                replacementColor = color
                                replacementColorDisplay.background = color
                                replacementColorDisplay.repaint()
                            }
                        } catch (_: Exception) {
                            // Invalid color format
                        }
                    }
                })

                replacementColorButton.addActionListener {
                    val color = ColorChooserService.instance.showDialog(
                        toolWindow.project,
                        this@apply,
                        PaletteerBundle.message("toolWindow.replace.selectColor"),
                        replacementColor,
                        true
                    )
                    if (color != null) {
                        replacementColor = color
                        replacementColorDisplay.background = color
                        replacementColorTextField.text = String.format("%06X", color.rgb and 0xFFFFFF)
                    }
                }

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

            add(contentPanel, BorderLayout.CENTER)
        }

        private fun performColorReplacement() {
            val original = originalColor
            val replacement = replacementColor

            if (original == null || replacement == null) {
                Messages.showWarningDialog(
                    toolWindow.project,
                    PaletteerBundle.message("toolWindow.replace.error.selectColors"),
                    PaletteerBundle.message("toolWindow.replace.error.title")
                )
                return
            }

            val scheme = EditorColorsManager.getInstance().globalScheme
            val replaced = replaceColorsInScheme(scheme, original, replacement)

            Messages.showInfoMessage(
                toolWindow.project,
                PaletteerBundle.message("toolWindow.replace.success", replaced),
                PaletteerBundle.message("toolWindow.replace.success.title")
            )
        }

        private fun replaceColorsInScheme(scheme: EditorColorsScheme, original: Color, replacement: Color): Int {
            var replacedCount = 0

            // Replace colors in attributesMap (text token colors)
            if (includeTextAttributes) {
                val attributesMap = TextAttributesKey.getAllKeys().associateWith { scheme.getAttributes(it) }
                attributesMap.forEach { (attributeKey, value) ->
                    if (value == null) return@forEach

                    var modified = false
                    val attrs = value.clone()

                    if (attrs.foregroundColor == original) {
                        attrs.foregroundColor = replacement
                        modified = true
                    }
                    if (attrs.backgroundColor == original) {
                        attrs.backgroundColor = replacement
                        modified = true
                    }
                    if (attrs.effectColor == original) {
                        attrs.effectColor = replacement
                        modified = true
                    }
                    if (attrs.errorStripeColor == original) {
                        attrs.errorStripeColor = replacement
                        modified = true
                    }

                    if (modified) {
                        scheme.setAttributes(attributeKey, attrs)
                        replacedCount++
                    }
                }
            }

            // Replace colors in colorMap (editor UI colors)
            if (includeEditorColors) {
                val colorMap = (scheme as? com.intellij.openapi.editor.colors.impl.AbstractColorsScheme)?.colorKeys
                colorMap?.forEach { colorKey ->
                    val color = scheme.getColor(colorKey)
                    if (color == original) {
                        scheme.setColor(colorKey as com.intellij.openapi.editor.colors.ColorKey, replacement)
                        replacedCount++
                    }
                }
            }

            return replacedCount
        }
    }
}
