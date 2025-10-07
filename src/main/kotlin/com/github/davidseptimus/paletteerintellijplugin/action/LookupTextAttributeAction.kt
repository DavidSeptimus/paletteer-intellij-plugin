package com.github.davidseptimus.paletteerintellijplugin.action

import com.github.davidseptimus.paletteerintellijplugin.PaletteerBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.CommonProcessors

class LookupTextAttributeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val offset = editor.caretModel.offset

        if (offset < 0 || offset >= editor.document.textLength) {
            Messages.showWarningDialog(
                project,
                PaletteerBundle.message("lookup.dialog.caretInvalid"),
                PaletteerBundle.message("lookup.dialog.title")
            )
            return
        }

        val editorImpl = editor as? EditorImpl
        if (editorImpl == null) {
            Messages.showWarningDialog(
                project,
                PaletteerBundle.message("lookup.dialog.editorNotSupported"),
                PaletteerBundle.message("lookup.dialog.title")
            )
            return
        }

        val results = mutableListOf<TextAttributeDialog.AttributeInfo>()

        // Process markup model (range highlighters)
        processMarkupModel(results, editorImpl, offset)

        // Process syntax model (syntax highlighting)
        processSyntaxModel(results, editorImpl, offset)

        // Remove duplicates based on key + foreground + background
        val uniqueResults = results.distinctBy {
            Triple(it.key, it.foreground, it.background)
        }

        if (uniqueResults.isEmpty()) {
            Messages.showInfoMessage(
                project,
                PaletteerBundle.message("lookup.dialog.noAttributes"),
                PaletteerBundle.message("lookup.dialog.title")
            )
            return
        }

        TextAttributeDialog(uniqueResults).show()
    }

    private fun processMarkupModel(results: MutableList<TextAttributeDialog.AttributeInfo>, editor: EditorImpl, offset: Int) {
        val editorModel = editor.markupModel
        val documentModel = editor.filteredDocumentMarkupModel

        val processor = CommonProcessors.CollectProcessor<RangeHighlighterEx>()
        editorModel.processRangeHighlightersOverlappingWith(offset, offset + 1, processor)
        documentModel.processRangeHighlightersOverlappingWith(offset, offset + 1, processor)

        val highlighters = processor.results.sortedByDescending { it.layer }

        for (highlighter in highlighters) {
            val text = editor.document.immutableCharSequence.substring(highlighter.startOffset, highlighter.endOffset)
            val key = highlighter.textAttributesKey

            // Skip IDENTIFIER_UNDER_CARET_ATTRIBUTES
            if (key?.externalName == "IDENTIFIER_UNDER_CARET_ATTRIBUTES") {
                continue
            }

            val attributes = highlighter.textAttributes ?: highlighter.forcedTextAttributes

            if (key != null || attributes != null) {
                val schemeAttrs = key?.let { editor.colorsScheme.getAttributes(it) } ?: attributes
                if (schemeAttrs != null) {
                    results.add(TextAttributeDialog.AttributeInfo(
                        type = PaletteerBundle.message("lookup.dialog.type.markup"),
                        text = text,
                        key = key?.externalName,
                        foreground = schemeAttrs.foregroundColor,
                        background = schemeAttrs.backgroundColor,
                    ))
                }
            }
        }
    }

    private fun processSyntaxModel(results: MutableList<TextAttributeDialog.AttributeInfo>, editor: EditorImpl, offset: Int) {
        val iterator = editor.highlighter.createIterator(offset)

        if (!iterator.atEnd() && iterator.start <= offset && offset < iterator.end) {
            val text = editor.document.immutableCharSequence.substring(iterator.start, iterator.end)
            val keys = iterator.textAttributesKeys

            keys.forEach { key ->
                // Skip IDENTIFIER_UNDER_CARET_ATTRIBUTES
                if (key.externalName == "IDENTIFIER_UNDER_CARET_ATTRIBUTES") {
                    return@forEach
                }

                val schemeAttrs = editor.colorsScheme.getAttributes(key)
                if (schemeAttrs != null) {
                    results.add(TextAttributeDialog.AttributeInfo(
                        type = PaletteerBundle.message("lookup.dialog.type.syntax"),
                        text = text.toString(),
                        key = key.externalName,
                        foreground = schemeAttrs.foregroundColor,
                        background = schemeAttrs.backgroundColor,
                    ))
                }
            }
        }
    }
}
