package com.github.davidseptimus.paletteerintellijplugin.toolWindow

import com.github.davidseptimus.paletteerintellijplugin.PaletteerBundle
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JTable
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * Panel for searching and displaying text attributes and editor colors.
 */
class LookupPanel : JBPanel<JBPanel<*>>() {

    private var useRegex = false
    private var lookupTextAttributes = true
    private val searchHistory = mutableListOf<String>()
    private var historyIndex = -1

    private val tableModel = object : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    init {
        layout = BorderLayout()
        add(JBLabel(PaletteerBundle.message("toolWindow.lookup")).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }, BorderLayout.NORTH)

        val searchField = JBTextField()

        // Controls panel
        val controlsPanel = createControlsPanel(searchField)
        add(controlsPanel, BorderLayout.NORTH)

        // Results table
        val resultsTable = createResultsTable()
        val scrollPane = JBScrollPane(resultsTable)
        add(scrollPane, BorderLayout.CENTER)

        // Setup search functionality
        setupSearchField(searchField)

        // Initial search to populate table
        performSearch("")
    }

    private fun createControlsPanel(searchField: JBTextField): JBPanel<JBPanel<*>> {
        return JBPanel<JBPanel<*>>().apply {
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
            val optionsPanel = createOptionsPanel()
            add(optionsPanel, gbc)
        }
    }

    private fun createOptionsPanel(): JBPanel<JBPanel<*>> {
        return JBPanel<JBPanel<*>>().apply {
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
    }

    private fun createResultsTable(): JBTable {
        tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.key"))
        tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.foreground"))
        tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.background"))
        tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.effects"))
        tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.bold"))
        tableModel.addColumn(PaletteerBundle.message("toolWindow.lookup.table.italic"))

        return JBTable(tableModel).apply {
            // Use transparent selection background
            val transparentSelection = Color(
                JBColor.BLUE.red,
                JBColor.BLUE.green,
                JBColor.BLUE.blue,
                30 // Alpha value for transparency
            )
            selectionBackground = transparentSelection
            selectionForeground = foreground

            setDefaultRenderer(Any::class.java, createCellRenderer(transparentSelection))

            // Use checkbox renderer for Bold and Italic columns
            val checkboxRenderer = createCheckboxRenderer(transparentSelection)
            columnModel.getColumn(4).cellRenderer = checkboxRenderer
            columnModel.getColumn(5).cellRenderer = checkboxRenderer
        }
    }

    private fun createCellRenderer(transparentSelection: Color) = object : DefaultTableCellRenderer() {
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
    }

    private fun createCheckboxRenderer(transparentSelection: Color) = object : DefaultTableCellRenderer() {
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

    private fun setupSearchField(searchField: JBTextField) {
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
            override fun insertUpdate(e: DocumentEvent?) = performSearch(searchField.text)
            override fun removeUpdate(e: DocumentEvent?) = performSearch(searchField.text)
            override fun changedUpdate(e: DocumentEvent?) = performSearch(searchField.text)
        })
    }

    private fun performSearch(query: String) {
        tableModel.rowCount = 0

        val scheme = EditorColorsManager.getInstance().globalScheme

        if (lookupTextAttributes) {
            searchTextAttributes(query, scheme)
        } else {
            searchEditorColors(query, scheme)
        }
    }

    private fun searchTextAttributes(query: String, scheme: com.intellij.openapi.editor.colors.EditorColorsScheme) {
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
    }

    private fun searchEditorColors(query: String, scheme: com.intellij.openapi.editor.colors.EditorColorsScheme) {
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
}

