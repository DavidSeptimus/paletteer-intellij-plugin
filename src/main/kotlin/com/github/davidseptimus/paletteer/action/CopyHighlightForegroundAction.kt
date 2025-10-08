package com.github.davidseptimus.paletteer.action

import com.github.davidseptimus.paletteer.PaletteerNotifier
import com.github.davidseptimus.paletteer.util.EditorAttributeUtils
import com.github.davidseptimus.paletteer.util.toHex
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

        val color = EditorAttributeUtils.findVisibleForegroundAttributeAtOffset(editorImpl, offset, scheme)?.attributes?.foregroundColor

        if (color != null) {
            val hex = color.toHex()
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(hex), null)
        } else {
           PaletteerNotifier.notifyInfo(project, "No foreground color found at caret position.")
        }
    }
}

