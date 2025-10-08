package com.github.davidseptimus.paletteer.toolWindow

import com.github.davidseptimus.paletteer.PaletteerBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.ColorPicker
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.CommonProcessors
import com.intellij.ui.JBColor
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
import java.awt.Font
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

/**
 * Data class representing a row in the attributes/colors table.
 */
data class AttributeRow(
    val key: String,
    var foreground: Color?,
    var background: Color?,
    var effectColor: Color?,
    var effects: String,
    var bold: Boolean,
    var italic: Boolean
)

/**
 * Cell editor for color columns using ColorChooser.
 */
private class ColorCellEditor : AbstractCellEditor(), TableCellEditor {
    private var currentColor: Color? = null

    override fun getCellEditorValue(): Any? = currentColor

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        currentColor = value as? Color

        // Show color picker dialog
        SwingUtilities.invokeLater {
            val newColor = ColorPicker.showDialog(
                table,
                PaletteerBundle.message("toolWindow.replace.pickColor"),
                currentColor,
                true,
                null,
                false
            )
            if (newColor != null) {
                currentColor = newColor
                stopCellEditing()
            } else {
                cancelCellEditing()
            }
        }

        // Return a temporary component (won't be visible long)
        return JLabel()
    }
}

/**
 * Cell editor for effect type dropdown.
 */
private class EffectCellEditor : AbstractCellEditor(), TableCellEditor {
    private val comboBox = JComboBox<String>(
        arrayOf(
            "",
            "LINE_UNDERSCORE",
            "BOLD_LINE_UNDERSCORE",
            "WAVE_UNDERSCORE",
            "STRIKEOUT",
            "BOLD_DOTTED_LINE"
        )
    ).apply {
        // Stop editing immediately when selection changes
        addActionListener {
            stopCellEditing()
        }
    }

    override fun getCellEditorValue(): Any = comboBox.selectedItem as String

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        comboBox.selectedItem = value as? String ?: ""
        return comboBox
    }
}

/**
 * Cell editor for boolean columns using checkboxes.
 */
private class BooleanCellEditor : AbstractCellEditor(), TableCellEditor {
    private val checkBox = JBCheckBox().apply {
        horizontalAlignment = SwingConstants.CENTER
        // Stop editing immediately when checkbox is clicked
        addActionListener {
            stopCellEditing()
        }
    }

    override fun getCellEditorValue(): Any = checkBox.isSelected

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        checkBox.isSelected = value as? Boolean ?: false
        return checkBox
    }
}

/**
 * Column definitions for the table.
 */
private class KeyColumn : ColumnInfo<AttributeRow, String>(PaletteerBundle.message("toolWindow.lookup.table.key")) {
    override fun valueOf(item: AttributeRow) = item.key
}

private class ForegroundColumn(private val onUpdate: () -> Unit) :
    ColumnInfo<AttributeRow, Color?>(PaletteerBundle.message("toolWindow.lookup.table.foreground")) {
    override fun valueOf(item: AttributeRow) = item.foreground
    override fun getRenderer(item: AttributeRow?) = ColorCellRenderer()
    override fun isCellEditable(item: AttributeRow?) = true
    override fun getEditor(item: AttributeRow?): TableCellEditor = ColorCellEditor()
    override fun setValue(item: AttributeRow, value: Color?) {
        item.foreground = value
        updateScheme(item)
        onUpdate()
    }
}

private class BackgroundColumn(private val onUpdate: () -> Unit) :
    ColumnInfo<AttributeRow, Color?>(PaletteerBundle.message("toolWindow.lookup.table.background")) {
    override fun valueOf(item: AttributeRow) = item.background
    override fun getRenderer(item: AttributeRow?) = ColorCellRenderer()
    override fun isCellEditable(item: AttributeRow?) = true
    override fun getEditor(item: AttributeRow?): TableCellEditor = ColorCellEditor()
    override fun setValue(item: AttributeRow, value: Color?) {
        item.background = value
        updateScheme(item)
        onUpdate()
    }
}

private class EffectColorColumn(private val onUpdate: () -> Unit) :
    ColumnInfo<AttributeRow, Color?>(PaletteerBundle.message("toolWindow.lookup.table.effectColor")) {
    override fun valueOf(item: AttributeRow) = item.effectColor
    override fun getRenderer(item: AttributeRow?) = ColorCellRenderer()
    override fun isCellEditable(item: AttributeRow?) = true
    override fun getEditor(item: AttributeRow?): TableCellEditor = ColorCellEditor()
    override fun setValue(item: AttributeRow, value: Color?) {
        item.effectColor = value
        updateScheme(item)
        onUpdate()
    }
}

private class EffectsColumn(private val onUpdate: () -> Unit) :
    ColumnInfo<AttributeRow, String>(PaletteerBundle.message("toolWindow.lookup.table.effects")) {
    override fun valueOf(item: AttributeRow) = item.effects
    override fun isCellEditable(item: AttributeRow?) = true
    override fun getEditor(item: AttributeRow?): TableCellEditor = EffectCellEditor()
    override fun setValue(item: AttributeRow, value: String) {
        item.effects = value
        updateScheme(item)
        onUpdate()
    }
}

private class BoldColumn(private val onUpdate: () -> Unit) :
    ColumnInfo<AttributeRow, Boolean>(PaletteerBundle.message("toolWindow.lookup.table.bold")) {
    override fun valueOf(item: AttributeRow) = item.bold
    override fun getRenderer(item: AttributeRow?) = BooleanCellRenderer()
    override fun isCellEditable(item: AttributeRow?) = true
    override fun getEditor(item: AttributeRow?): TableCellEditor = BooleanCellEditor()
    override fun setValue(item: AttributeRow, value: Boolean) {
        item.bold = value
        updateScheme(item)
        onUpdate()
    }
}

private class ItalicColumn(private val onUpdate: () -> Unit) :
    ColumnInfo<AttributeRow, Boolean>(PaletteerBundle.message("toolWindow.lookup.table.italic")) {
    override fun valueOf(item: AttributeRow) = item.italic
    override fun getRenderer(item: AttributeRow?) = BooleanCellRenderer()
    override fun isCellEditable(item: AttributeRow?) = true
    override fun getEditor(item: AttributeRow?): TableCellEditor = BooleanCellEditor()
    override fun setValue(item: AttributeRow, value: Boolean) {
        item.italic = value
        updateScheme(item)
        onUpdate()
    }
}

/**
 * Updates the color scheme with the modified attribute row using ColorReplacementService.
 */
private fun updateScheme(item: AttributeRow) {
    val scheme = EditorColorsManager.getInstance().globalScheme
    val colorReplacementService = ColorReplacementService()

    // Calculate font type from bold and italic flags
    val fontType = (if (item.bold) Font.BOLD else 0) or (if (item.italic) Font.ITALIC else 0)

    // Parse effect type from string
    val effectType = when (item.effects) {
        "LINE_UNDERSCORE" -> EffectType.LINE_UNDERSCORE
        "BOLD_LINE_UNDERSCORE" -> EffectType.BOLD_LINE_UNDERSCORE
        "WAVE_UNDERSCORE" -> EffectType.WAVE_UNDERSCORE
        "STRIKEOUT" -> EffectType.STRIKEOUT
        "BOLD_DOTTED_LINE" -> EffectType.BOLD_DOTTED_LINE
        else -> null
    }

    // Update the attribute using the service (this will also force reload)
    colorReplacementService.updateTextAttribute(
        scheme,
        item.key,
        item.foreground,
        item.background,
        item.effectColor,
        effectType,
        fontType
    )
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
 * Custom extension that supports selected state with different icons
 */
private class SelectableExtension(
    private val normalIcon: Icon,
    private val selectedIcon: Icon,
    private val tooltip: String,
    private val action: () -> Unit,
    private val isSelectedProvider: () -> Boolean
) : ExtendableTextComponent.Extension {

    override fun getIcon(hovered: Boolean): Icon {
        return if (isSelected()) selectedIcon else normalIcon
    }

    override fun isSelected(): Boolean {
        return isSelectedProvider()
    }

    override fun getTooltip(): String = tooltip

    override fun getActionOnClick(): Runnable = Runnable { action() }

    override fun isIconBeforeText(): Boolean = false
}

/**
 * Panel for searching and displaying text attributes and editor colors.
 */
class LookupPanel(private val project: Project) : JBPanel<JBPanel<*>>() {

    val searchModel = SearchModel()

    private val tableModel: ListTableModel<AttributeRow>
    private var resultsTable: TableView<AttributeRow>? = null
    private var caretListener: CaretListener? = null
    private var currentEditor: Editor? = null
    private var historyDebounceTimer: Timer? = null

    private val searchField = ExtendableTextField().apply {
        emptyText.text = PaletteerBundle.message("toolWindow.lookup.searchPlaceholder")

        // Create the search history extension button
        val historyExtension = object : ExtendableTextComponent.Extension {
            override fun getIcon(hovered: Boolean): Icon {
                return if (SearchHistoryService.getInstance().getSearchHistory().isNotEmpty()) {
                    AllIcons.Actions.SearchWithHistory
                } else {
                    AllIcons.Actions.Search
                }
            }

            override fun getTooltip(): String = PaletteerBundle.message("toolWindow.lookup.searchHistory")
            override fun getActionOnClick(): Runnable = Runnable { showSearchHistoryPopup() }
            override fun isIconBeforeText(): Boolean = true
        }

        // Create the use regex extension button
        val useRegexExtension = SelectableExtension(
            AllIcons.Actions.Regex,
            AllIcons.Actions.RegexSelected,
            PaletteerBundle.message("toolWindow.lookup.useRegex"),
            {
                searchModel.useRegex = !searchModel.useRegex
                if (searchModel.useRegex) {
                    searchModel.followCaret = false
                    removeCaretListener()
                }
                repaint()
                performSearch(text)
            },
            { searchModel.useRegex }
        )

        // Create the follow caret extension button
        val followCaretExtension = SelectableExtension(
            AllIcons.Actions.RunToCursor,
            AllIcons.Actions.RunToCursor,
            PaletteerBundle.message("toolWindow.lookup.followCaret"),
            {
                searchModel.followCaret = !searchModel.followCaret
                repaint()
                if (searchModel.followCaret) {
                    searchModel.useRegex = false
                    setupCaretListener()
                } else {
                    removeCaretListener()
                }
            },
            { searchModel.followCaret }
        )

        addExtension(historyExtension)
        addExtension(useRegexExtension)
        addExtension(followCaretExtension)
    }

    init {
        layout = BorderLayout()

        // Initialize table model with update callback
        tableModel = ListTableModel<AttributeRow>(
            arrayOf(
                KeyColumn(),
                ForegroundColumn { resultsTable?.repaint() },
                BackgroundColumn { resultsTable?.repaint() },
                EffectColorColumn { resultsTable?.repaint() },
                EffectsColumn { resultsTable?.repaint() },
                BoldColumn { resultsTable?.repaint() },
                ItalicColumn { resultsTable?.repaint() }
            ),
            mutableListOf()
        )

        // Controls panel using UI DSL
        val controlsPanel = createControlsPanel(searchModel)
        add(controlsPanel, BorderLayout.NORTH)

        // Results table
        resultsTable = createResultsTable()
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


    private fun createResultsTable(): TableView<AttributeRow> {
        val table = TableView(tableModel)
        table.setCellSelectionEnabled(true)
        table.inputMap.put(javax.swing.KeyStroke.getKeyStroke("meta C"), "copyCell")
        table.actionMap.put("copyCell", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val row = table.selectedRow
                val col = table.selectedColumn
                if (row >= 0 && col >= 0) {
                    val value = table.getValueAt(row, col)
                    val text = when (value) {
                        is Color -> String.format("%06X", value.rgb and 0xFFFFFF)
                        is Boolean -> value.toString()
                        null -> ""
                        else -> value.toString()
                    }
                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                    val selection = java.awt.datatransfer.StringSelection(text)
                    clipboard.setContents(selection, selection)
                }
            }
        })

        // Adjust column widths: key column widest, others shrink to fit
        val columnModel = table.columnModel
        if (columnModel.columnCount > 0) {
            // Key column (first column)
            columnModel.getColumn(0).preferredWidth = 300
            columnModel.getColumn(0).minWidth = 150
            columnModel.getColumn(0).maxWidth = 1000
            columnModel.getColumn(0).resizable = true
            // Other columns
            for (i in 1 until columnModel.columnCount) {
                columnModel.getColumn(i).preferredWidth = 60
                columnModel.getColumn(i).minWidth = 40
                columnModel.getColumn(i).maxWidth = 120
                columnModel.getColumn(i).resizable = false
            }
        }

        return table
    }

    private fun setupSearchField() {
        // Search on text change
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                performSearch(searchField.text)
                scheduleHistoryCapture(searchField.text)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                performSearch(searchField.text)
                scheduleHistoryCapture(searchField.text)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                performSearch(searchField.text)
                scheduleHistoryCapture(searchField.text)
            }
        })
    }

    /**
     * Schedule history capture after 2 seconds of no typing.
     */
    private fun scheduleHistoryCapture(query: String) {
        // Cancel any existing timer
        historyDebounceTimer?.stop()
        historyDebounceTimer = null

        // Only schedule if query is not blank and not following caret
        if (query.isNotBlank() && !searchModel.followCaret) {
            historyDebounceTimer = Timer(2000) {
                SearchHistoryService.getInstance().addSearch(query)
            }.apply {
                isRepeats = false
                start()
            }
        }
    }

    private fun performSearch(query: String) {
        val results = mutableListOf<AttributeRow>()

        val scheme = EditorColorsManager.getInstance().globalScheme

        if (searchModel.lookupTextAttributes) {
            searchTextAttributes(query, scheme, results, searchModel.useRegex)
        } else {
            searchEditorColors(query, scheme, results, searchModel.useRegex)
        }

        // Sort results alphabetically by key (case-insensitive, nulls last)
        val sortedResults = results.sortedWith(compareBy({ it.key.lowercase() }, { it.key == null }))
        tableModel.items = sortedResults
    }

    /**
     * Show popup with search history.
     */
    private fun showSearchHistoryPopup() {
        val history = SearchHistoryService.getInstance().getSearchHistory()

        if (history.isEmpty()) {
            // Show empty state popup
            val emptyPopup = JBPopupFactory.getInstance()
                .createMessage(PaletteerBundle.message("toolWindow.lookup.searchHistory.empty"))
            emptyPopup.showUnderneathOf(searchField)
            return
        }

        // Add 'Clear history' entry at the top
        val clearHistoryLabel = PaletteerBundle.message("toolWindow.lookup.searchHistory.clear")
        val items = mutableListOf<String>()
        items.add(clearHistoryLabel)
        items.addAll(history)

        val popupStep = object : BaseListPopupStep<String>(
            PaletteerBundle.message("toolWindow.lookup.searchHistory"),
            items
        ) {
            override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice && selectedValue != null) {
                    if (selectedValue == clearHistoryLabel) {
                        SearchHistoryService.getInstance().clearHistory()
                        return FINAL_CHOICE
                    } else {
                        searchField.text = selectedValue
                        performSearch(selectedValue)
                    }
                }
                return null
            }

            override fun hasSubstep(selectedValue: String?): Boolean = false
            override fun isSpeedSearchEnabled(): Boolean = true
        }

        val popup = JBPopupFactory.getInstance().createListPopup(popupStep)
        popup.showUnderneathOf(searchField)
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
                    val effectColor = attrs.effectColor
                    val effects = if (effectColor == null) "" else attrs.effectType?.toString() ?: ""
                    results.add(
                        AttributeRow(
                            key = keyName,
                            foreground = attrs.foregroundColor,
                            background = attrs.backgroundColor,
                            effectColor = effectColor,
                            effects = effects,
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
        scheme: EditorColorsScheme,
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
                        effectColor = null,
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

    private fun setupCaretListener() {
        removeCaretListener()

        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        currentEditor = editor

        caretListener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                updateSearchFromCaret(editor)
            }
        }

        editor.caretModel.addCaretListener(caretListener!!)
        updateSearchFromCaret(editor)
    }

    private fun removeCaretListener() {
        caretListener?.let { listener ->
            currentEditor?.caretModel?.removeCaretListener(listener)
        }
        caretListener = null
        currentEditor = null
    }

    private fun updateSearchFromCaret(editor: Editor) {
        val offset = editor.caretModel.offset
        val editorImpl = editor as? EditorImpl ?: return

        val results = mutableListOf<String>()

        // Process markup model
        val editorModel = editorImpl.markupModel
        val documentModel = editorImpl.filteredDocumentMarkupModel
        val processor = CommonProcessors.CollectProcessor<RangeHighlighterEx>()

        editorModel.processRangeHighlightersOverlappingWith(offset, offset + 1, processor)
        documentModel.processRangeHighlightersOverlappingWith(offset, offset + 1, processor)

        val highlighters = processor.results.sortedByDescending { it.layer }
        for (highlighter in highlighters) {
            val key = highlighter.textAttributesKey
            if (key?.externalName != null && key.externalName != "IDENTIFIER_UNDER_CARET_ATTRIBUTES") {
                results.add(key.externalName)
            }
        }

        // Process syntax model
        val iterator = editorImpl.highlighter.createIterator(offset)
        if (!iterator.atEnd() && iterator.start <= offset && offset < iterator.end) {
            val keys = iterator.textAttributesKeys
            keys.forEach { key ->
                if (key.externalName != "IDENTIFIER_UNDER_CARET_ATTRIBUTES") {
                    results.add(key.externalName)
                }
            }
        }

        // Update search field with the first result
        if (results.isNotEmpty()) {
            val newText = results.first()
            searchField.text = newText
            if (newText.isNotBlank()) {
                SearchHistoryService.getInstance().addSearch(newText)
            }
        }
    }
}


data class SearchModel(
    var useRegex: Boolean = false,
    var followCaret: Boolean = false,
    var lookupTextAttributes: Boolean = true,
)
