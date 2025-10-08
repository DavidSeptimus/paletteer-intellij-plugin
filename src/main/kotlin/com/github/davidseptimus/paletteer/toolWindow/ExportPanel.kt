package com.github.davidseptimus.paletteer.toolWindow

import com.github.davidseptimus.paletteer.PaletteerBundle
import com.github.davidseptimus.paletteer.util.toHex
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.io.File

/**
 * Panel for exporting color scheme data in various formats.
 */
class ExportPanel(private val project: Project) : Disposable {

    companion object {
        private const val EXTENSION_PROPERTY_KEY = "paletteer.export.extension"
        private const val INCLUDE_EDITOR_FONT_KEY = "paletteer.export.includeEditorFont"
        private const val INCLUDE_TERMINAL_FONT_KEY = "paletteer.export.includeTerminalFont"
        private const val DEFAULT_EXTENSION = "xml"
    }

    private var schemeName: String = EditorColorsManager.getInstance().globalScheme.displayName
    private var selectedExtension: String? =
        PropertiesComponent.getInstance().getValue(EXTENSION_PROPERTY_KEY, DEFAULT_EXTENSION)
    private var includeEditorFont: Boolean =
        PropertiesComponent.getInstance().getBoolean(INCLUDE_EDITOR_FONT_KEY, false)
    private var includeTerminalFont: Boolean =
        PropertiesComponent.getInstance().getBoolean(INCLUDE_TERMINAL_FONT_KEY, false)

    private val formPanel = panel {
        group(PaletteerBundle.message("toolWindow.export.title")) {
            row(PaletteerBundle.message("toolWindow.export.schemeName")) {
                textField()
                    .bindText(::schemeName)
                    .resizableColumn()
            }
            row(PaletteerBundle.message("toolWindow.export.extension")) {
                comboBox(listOf("xml", "icls"))
                    .bindItem(::selectedExtension)
            }
            group(PaletteerBundle.message("toolWindow.export.fontSettings")) {
                row {
                    checkBox(PaletteerBundle.message("toolWindow.export.includeEditorFont"))
                        .bindSelected(::includeEditorFont)
                }
                row {
                    checkBox(PaletteerBundle.message("toolWindow.export.includeTerminalFont"))
                        .bindSelected(::includeTerminalFont)
                }
            }
            row {
                button(PaletteerBundle.message("toolWindow.export.button")) {
                    performExport()
                }
            }
        }
    }

    val component = JBScrollPane(formPanel).apply {
        border = JBUI.Borders.empty(4, 12, 8, 0)
    }

    private fun performExport() {
        // Apply panel changes to sync bindings
        formPanel.apply()

        // Validate inputs
        if (schemeName.isBlank()) {
            Messages.showErrorDialog(
                project,
                PaletteerBundle.message("toolWindow.export.error.noName"),
                PaletteerBundle.message("toolWindow.export.error.title")
            )
            return
        }

        val extension = selectedExtension ?: DEFAULT_EXTENSION

        // Save preferences
        val properties = PropertiesComponent.getInstance()
        properties.setValue(EXTENSION_PROPERTY_KEY, extension)
        properties.setValue(INCLUDE_EDITOR_FONT_KEY, includeEditorFont)
        properties.setValue(INCLUDE_TERMINAL_FONT_KEY, includeTerminalFont)

        // Show file save dialog
        val descriptor = FileSaverDescriptor(
            PaletteerBundle.message("toolWindow.export.dialogTitle"),
            "",
            extension
        )

        val fileChooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val defaultFileName = "$schemeName.$extension"

        val fileWrapper = fileChooser.save(null as com.intellij.openapi.vfs.VirtualFile?, defaultFileName)

        if (fileWrapper != null) {
            try {
                val currentScheme = EditorColorsManager.getInstance().globalScheme
                val file = fileWrapper.file

                exportColorScheme(currentScheme, file, schemeName, includeEditorFont, includeTerminalFont)

                // Create notification with action to open file in editor
                val notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("Paletteer Notifications")
                    .createNotification(
                        PaletteerBundle.message("toolWindow.export.success", file.absolutePath),
                        NotificationType.INFORMATION
                    )

                notification.addAction(NotificationAction.createSimple(
                    PaletteerBundle.message("toolWindow.export.openInEditor")
                ) {
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)?.let { virtualFile ->
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    }
                })

                notification.notify(project)
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    PaletteerBundle.message("toolWindow.export.error.exportFailed", e.message ?: "Unknown error"),
                    PaletteerBundle.message("toolWindow.export.error.title")
                )
            }
        }
    }

    private fun exportColorScheme(
        scheme: com.intellij.openapi.editor.colors.EditorColorsScheme,
        file: File,
        name: String,
        includeEditorFont: Boolean,
        includeTerminalFont: Boolean
    ) {
        // Export color scheme to XML format
        val xml = buildColorSchemeXml(scheme, name, includeEditorFont, includeTerminalFont)
        file.writeText(xml)
    }

    private fun buildColorSchemeXml(
        scheme: com.intellij.openapi.editor.colors.EditorColorsScheme,
        name: String,
        includeEditorFont: Boolean,
        includeTerminalFont: Boolean
    ): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")

        // Get the parent scheme name if available
        val parentSchemeName = (scheme as? com.intellij.openapi.editor.colors.impl.AbstractColorsScheme)
            ?.parentScheme?.name ?: "Default"

        sb.appendLine("<scheme name=\"$name\" version=\"142\" parent_scheme=\"$parentSchemeName\">")

        // Export font settings if requested
        if (includeEditorFont) {
            sb.appendLine("  <option name=\"FONT_SCALE\" value=\"1.0\" />")
            sb.appendLine("  <option name=\"LINE_SPACING\" value=\"${scheme.lineSpacing}\" />")
            sb.appendLine("  <option name=\"EDITOR_LIGATURES\" value=\"${scheme.fontPreferences.useLigatures()}\" />")

            val editorFontPrefs = scheme.fontPreferences
            val fonts = editorFontPrefs.realFontFamilies
            if (fonts.isNotEmpty()) {
                fonts.forEach { fontFamily ->
                    sb.appendLine("  <font>")
                    sb.appendLine("    <option name=\"EDITOR_FONT_SIZE\" value=\"${fontFamily}\" />")
                    sb.appendLine("    <option name=\"EDITOR_FONT_NAME\" value=\"${scheme.editorFontSize}\" />")
                    sb.appendLine("  </font>")
                }
            }
        }

        if (includeTerminalFont) {
            val consoleFontPrefs = scheme.consoleFontPreferences
            val consoneFonts = consoleFontPrefs.realFontFamilies
            if (consoneFonts.isNotEmpty()) {
                consoneFonts.forEach { fontFamily ->
                    sb.appendLine("  <console-font>")
                    sb.appendLine("    <option name=\"EDITOR_FONT_NAME\" value=\"$fontFamily\" />")
                    sb.appendLine("    <option name=\"EDITOR_FONT_SIZE\" value=\"${scheme.consoleFontSize}\" />")
                    sb.appendLine("  </console-font>")

                }
            }
            sb.appendLine("  <option name=\"CONSOLE_LIGATURES\" value=\"${consoleFontPrefs.useLigatures()}\" />")
            sb.appendLine("  <option name=\"CONSOLE_LINE_SPACING\" value=\"${scheme.consoleLineSpacing}\" />")
        }

        // Export colors
        sb.appendLine("  <colors>")
        val colorKeys = (scheme as? com.intellij.openapi.editor.colors.impl.AbstractColorsScheme)?.colorKeys
        colorKeys?.forEach { key ->
            val color = scheme.getColor(key as ColorKey)
            if (color != null) {
                val hex = color.toHex()
                sb.appendLine("    <option name=\"${key.externalName}\" value=\"$hex\" />")
            }
        }
        sb.appendLine("  </colors>")

        // Export text attributes
        sb.appendLine("  <attributes>")
        TextAttributesKey.getAllKeys().forEach { key ->
            val attrs = scheme.getAttributes(key)
            if (attrs != null) {
                sb.appendLine("    <option name=\"${key.externalName}\">")
                sb.appendLine("      <value>")

                attrs.foregroundColor?.let {
                    val hex = it.toHex()
                    sb.appendLine("        <option name=\"FOREGROUND\" value=\"$hex\" />")
                }

                attrs.backgroundColor?.let {
                    val hex = it.toHex()
                    sb.appendLine("        <option name=\"BACKGROUND\" value=\"$hex\" />")
                }

                if (attrs.fontType != 0) {
                    sb.appendLine("        <option name=\"FONT_TYPE\" value=\"${attrs.fontType}\" />")
                }

                attrs.effectColor?.let {
                    val hex = it.toHex()
                    sb.appendLine("        <option name=\"EFFECT_COLOR\" value=\"$hex\" />")
                }

                attrs.effectType?.let {
                    sb.appendLine("        <option name=\"EFFECT_TYPE\" value=\"${it.ordinal}\" />")
                }

                sb.appendLine("      </value>")
                sb.appendLine("    </option>")
            }
        }
        sb.appendLine("  </attributes>")
        sb.appendLine("</scheme>")
        return sb.toString()
    }

    override fun dispose() {
        // Clean up resources when the panel is disposed
    }
}