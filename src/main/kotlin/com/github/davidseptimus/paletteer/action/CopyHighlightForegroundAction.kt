package com.github.davidseptimus.paletteer.action

import com.github.davidseptimus.paletteer.PaletteerNotifier
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.CommonProcessors
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CopyHighlightForegroundAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val offset = editor.caretModel.offset
        val scheme = editor.colorsScheme

        val editorImpl = editor as? EditorImpl
        if (editorImpl == null) {
            Messages.showWarningDialog(
                project,
                "Editor not supported.",
                "Paletteer: Copy Foreground Color"
            )
            return
        }

        val editorModel = editorImpl.markupModel
        val documentModel = editorImpl.filteredDocumentMarkupModel
        val processor = CommonProcessors.CollectProcessor<RangeHighlighterEx>()
        editorModel.processRangeHighlightersOverlappingWith(offset, offset + 1, processor)
        documentModel.processRangeHighlightersOverlappingWith(offset, offset + 1, processor)

        // Outermost = highest layer WITH a text attribute foreground color
        val markupHighlighter = processor.results
            .filter {
                val attrs = it.getTextAttributes(scheme) ?: it.forcedTextAttributes
                attrs?.foregroundColor != null
            }
            .maxByOrNull { it.layer }
        val markupColor = markupHighlighter?.getTextAttributes(scheme)?.foregroundColor
            ?: markupHighlighter?.forcedTextAttributes?.foregroundColor

        var color = markupColor

        // If no color found in markup, check syntax highlighting
        if (color == null) {
            val iterator = editorImpl.highlighter.createIterator(offset)
            if (!iterator.atEnd() && iterator.start <= offset && offset < iterator.end) {
                val keys = iterator.textAttributesKeys
                for (key in keys) {
                    val schemeAttrs = scheme.getAttributes(key)
                    if (schemeAttrs?.foregroundColor != null) {
                        color = schemeAttrs.foregroundColor
                        break
                    }
                }
            }
        }

        if (color != null) {
            val hex = String.format("#%06X", color.rgb and 0xFFFFFF)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(hex), null)
        } else {
           PaletteerNotifier.notifyInfo(project, "No foreground color found at caret position.")
        }
    }
}
