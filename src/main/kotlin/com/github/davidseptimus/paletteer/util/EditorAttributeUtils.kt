package com.github.davidseptimus.paletteer.util

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.util.CommonProcessors
import com.jetbrains.rd.util.addUnique

object EditorAttributeUtils {
    data class TextAttributeMapping(
        val key: TextAttributesKey,
        val attributes: TextAttributes
    )

    fun findTextAttributesAtOffset(
        editor: EditorImpl,
        offset: Int,
        scheme: EditorColorsScheme
    ): List<TextAttributeMapping> {

        val results = mutableListOf<TextAttributeMapping>()

        val editorModel = editor.markupModel
        val documentModel = editor.filteredDocumentMarkupModel
        val processor = CommonProcessors.CollectProcessor<RangeHighlighterEx>()

        editorModel.processRangeHighlightersOverlappingWith(offset, offset + 1, processor)
        documentModel.processRangeHighlightersOverlappingWith(offset, offset + 1, processor)


        // Process syntax model
        val syntaxHighlighterIterator = editor.highlighter.createIterator(offset)
        if (!syntaxHighlighterIterator.atEnd() && syntaxHighlighterIterator.start <= offset && offset < syntaxHighlighterIterator.end) {
            val keys = syntaxHighlighterIterator.textAttributesKeys
            keys.forEach { key ->
                val attrs = scheme.getAttributes(key)
                results.add(TextAttributeMapping(key, attrs))
            }
        }

        // Process markup model
        val markupHighlighters = processor.results.sortedByDescending { it.layer }
        for (highlighter in markupHighlighters) {
            val key = highlighter.textAttributesKey
            if (key?.externalName != null) {
                val attrs = highlighter.getTextAttributes(scheme) ?: highlighter.forcedTextAttributes

                results.add(TextAttributeMapping(key, attrs ?: TextAttributes()))
            }
        }
        return results.distinct()
    }

    fun findVisibleForegroundAttributeAtOffset(
        editor: EditorImpl,
        offset: Int,
        scheme: EditorColorsScheme
    ): TextAttributeMapping? {
        val attributes = findTextAttributesAtOffset(editor, offset, scheme).filter {
            it.attributes.foregroundColor != null && it.key.externalName != "IDENTIFIER_UNDER_CARET_ATTRIBUTES"
        }
        return attributes.lastOrNull() { it.attributes.foregroundColor != null }
    }
}

