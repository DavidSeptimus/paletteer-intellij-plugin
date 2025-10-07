package com.github.davidseptimus.paletteerintellijplugin.toolWindow

import com.github.davidseptimus.paletteerintellijplugin.PaletteerBundle
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.TableCellRenderer

/**
 * Data class representing a row in the attributes/colors table.
 */
data class AttributeRow(
    val key: String,
    val foreground: Color?,
    val background: Color?,
    val effects: String,
    val bold: Boolean,
    val italic: Boolean
)

/**
 * Column definitions for the table.
 */
private class KeyColumn : ColumnInfo<AttributeRow, String>(PaletteerBundle.message("toolWindow.lookup.table.key")) {
    override fun valueOf(item: AttributeRow) = item.key
}

private class ForegroundColumn :
    ColumnInfo<AttributeRow, Color?>(PaletteerBundle.message("toolWindow.lookup.table.foreground")) {
    override fun valueOf(item: AttributeRow) = item.foreground
    override fun getRenderer(item: AttributeRow?) = ColorCellRenderer()
}

private class BackgroundColumn :
    ColumnInfo<AttributeRow, Color?>(PaletteerBundle.message("toolWindow.lookup.table.background")) {
    override fun valueOf(item: AttributeRow) = item.background
    override fun getRenderer(item: AttributeRow?) = ColorCellRenderer()
}

private class EffectsColumn :
    ColumnInfo<AttributeRow, String>(PaletteerBundle.message("toolWindow.lookup.table.effects")) {
    override fun valueOf(item: AttributeRow) = item.effects
}

private class BoldColumn : ColumnInfo<AttributeRow, Boolean>(PaletteerBundle.message("toolWindow.lookup.table.bold")) {
    override fun valueOf(item: AttributeRow) = item.bold
    override fun getRenderer(item: AttributeRow?) = BooleanCellRenderer()
}

private class ItalicColumn :
    ColumnInfo<AttributeRow, Boolean>(PaletteerBundle.message("toolWindow.lookup.table.italic")) {
    override fun valueOf(item: AttributeRow) = item.italic
    override fun getRenderer(item: AttributeRow?) = BooleanCellRenderer()
}

/**
 * Renderer for color columns showing color swatches.
 */
private class ColorCellRenderer : TableCellRenderer {
    private val label = JLabel().apply {
        isOpaque = true
        horizontalAlignment = SwingConstants.CENTER
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        if (value is Color) {
            label.background = value
            label.text = String.format("%06X", value.rgb and 0xFFFFFF)
            label.foreground = if (value.red + value.green + value.blue > 382) Color.BLACK else Color.WHITE
        } else {
            label.background = table.background
            label.text = ""
            label.foreground = table.foreground
        }

        label.border = if (isSelected) {
            BorderFactory.createLineBorder(JBColor.BLUE, 1)
        } else {
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        }

        return label
    }
}

/**
 * Renderer for boolean columns showing checkboxes.
 */
private class BooleanCellRenderer : TableCellRenderer {
    private val checkbox = JBCheckBox().apply {
        isEnabled = false
        horizontalAlignment = SwingConstants.CENTER
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        checkbox.isSelected = value as? Boolean ?: false
        checkbox.background = table.background

        checkbox.border = if (isSelected) {
            BorderFactory.createLineBorder(JBColor.BLUE, 1)
        } else {
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        }

        return checkbox
    }
}

/**
 * Panel for searching and displaying text attributes and editor colors.
 */
class LookupPanel : JBPanel<JBPanel<*>>() {

    val searchModel = SearchModel()

    private val tableModel = ListTableModel<AttributeRow>(
        arrayOf(
            KeyColumn(),
            ForegroundColumn(),
            BackgroundColumn(),
            EffectsColumn(),
            BoldColumn(),
            ItalicColumn()
        ),
        mutableListOf()
    )

    private val searchField = object : SearchTextField("Paletteer.LookupPanel.SearchHistory") {
        private val maxHistorySize = 15

        override fun addCurrentTextToHistory() {
            super.addCurrentTextToHistory()
            // Trim history to max size
            val history = history.toMutableList()
            if (history.size > maxHistorySize) {
                setHistory(history.takeLast(maxHistorySize))
            }
        }
    }.apply {
        textEditor.emptyText.text = PaletteerBundle.message("toolWindow.lookup.searchPlaceholder")
    }

    init {
        layout = BorderLayout()

        // Controls panel using UI DSL
        val controlsPanel = createControlsPanel(searchModel)
        add(controlsPanel, BorderLayout.NORTH)

        // Results table
        val resultsTable = createResultsTable()
        val scrollPane = JBScrollPane(resultsTable)
        add(scrollPane, BorderLayout.CENTER)

        // Setup search functionality
        setupSearchField()

        // Initial search to populate table
        performSearch("")
    }

    private fun createControlsPanel(searchModel: SearchModel) = panel {
        row {
            cell(searchField)
                .resizableColumn()
                .align(AlignX.FILL)
        }

        row {
            checkBox(PaletteerBundle.message("toolWindow.lookup.useRegex"))
                .onChanged { checkbox ->
                    searchModel.useRegex = checkbox.isSelected
                    performSearch(searchField.text)
                }
        }

        buttonsGroup {
            row {
                /**
                 * No idea why binding doesn't work here, have to do it manually :(
                 */
                radioButton(PaletteerBundle.message("toolWindow.lookup.textAttributes"), true)
                    .onChanged {
                        searchModel.lookupTextAttributes = it.isSelected
                        performSearch(searchField.text)
                    }
                radioButton(
                    PaletteerBundle.message("toolWindow.lookup.editorColors"),
                    false
                )

            }
        }.bind(searchModel::lookupTextAttributes)
    }


    private fun createResultsTable() = TableView(tableModel)

    private fun setupSearchField() {
        // Search on text change
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = performSearch(searchField.text)
            override fun removeUpdate(e: DocumentEvent?) = performSearch(searchField.text)
            override fun changedUpdate(e: DocumentEvent?) = performSearch(searchField.text)
        })
    }

    private fun performSearch(query: String) {
        val results = mutableListOf<AttributeRow>()

        val scheme = EditorColorsManager.getInstance().globalScheme

        if (searchModel.lookupTextAttributes) {
            searchTextAttributes(query, scheme, results, searchModel.useRegex)
        } else {
            searchEditorColors(query, scheme, results, searchModel.useRegex)
        }

        tableModel.items = results
    }

    private fun searchTextAttributes(
        query: String,
        scheme: EditorColorsScheme,
        results: MutableList<AttributeRow>,
        useRegex: Boolean = false
    ) {
        val attributeKeys = TextAttributesKey.getAllKeys()
        attributeKeys.forEach { key ->
            val keyName = key.externalName
            if (matchesSearch(keyName, query, useRegex)) {
                val attrs = scheme.getAttributes(key)
                if (attrs != null) {
                    results.add(
                        AttributeRow(
                            key = keyName,
                            foreground = attrs.foregroundColor,
                            background = attrs.backgroundColor,
                            effects = attrs.effectType?.toString() ?: "",
                            bold = attrs.fontType and 1 != 0,
                            italic = attrs.fontType and 2 != 0
                        )
                    )
                }
            }
        }
    }

    private fun searchEditorColors(
        query: String,
        scheme: com.intellij.openapi.editor.colors.EditorColorsScheme,
        results: MutableList<AttributeRow>,
        useRegex: Boolean = false
    ) {
        val colorKeys = (scheme as? com.intellij.openapi.editor.colors.impl.AbstractColorsScheme)?.colorKeys
        colorKeys?.forEach { key ->
            val keyName = key.externalName
            if (matchesSearch(keyName, query, useRegex)) {
                val color = scheme.getColor(key)
                results.add(
                    AttributeRow(
                        key = keyName,
                        foreground = color,
                        background = null,
                        effects = "",
                        bold = false,
                        italic = false
                    )
                )
            }
        }
    }

    private fun matchesSearch(text: String, query: String, useRegex: Boolean = false): Boolean {
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


data class SearchModel(
    var useRegex: Boolean = false,
    var lookupTextAttributes: Boolean = true,
)
