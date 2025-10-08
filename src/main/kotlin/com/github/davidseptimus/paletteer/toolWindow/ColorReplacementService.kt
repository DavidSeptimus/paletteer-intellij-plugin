package com.github.davidseptimus.paletteer.toolWindow

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import java.awt.Color

/**
 * Service responsible for replacing colors in an editor color scheme.
 */
class ColorReplacementService {

    /**
     * Replace all occurrences of a color in the scheme with a replacement color.
     *
     * @param scheme The editor color scheme to modify
     * @param original The color to replace
     * @param replacement The new color
     * @param includeTextAttributes Whether to replace colors in text attributes
     * @param includeEditorColors Whether to replace colors in editor UI colors
     * @return The number of replacements made
     */
    fun replaceColorsInScheme(
        scheme: EditorColorsScheme,
        original: Color,
        replacement: Color,
        includeTextAttributes: Boolean,
        includeEditorColors: Boolean
    ): Int {
        var replacedCount = 0

        // Replace colors in attributesMap (text token colors)
        if (includeTextAttributes) {
            replacedCount += replaceTextAttributeColors(scheme, original, replacement)
        }

        // Replace colors in colorMap (editor UI colors)
        if (includeEditorColors) {
            replacedCount += replaceEditorColors(scheme, original, replacement)
        }

        return replacedCount
    }

    private fun replaceTextAttributeColors(
        scheme: EditorColorsScheme,
        original: Color,
        replacement: Color
    ): Int {
        var replacedCount = 0
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

        return replacedCount
    }

    private fun replaceEditorColors(
        scheme: EditorColorsScheme,
        original: Color,
        replacement: Color
    ): Int {
        var replacedCount = 0
        val colorMap = (scheme as? com.intellij.openapi.editor.colors.impl.AbstractColorsScheme)?.colorKeys

        colorMap?.forEach { colorKey ->
            val color = scheme.getColor(colorKey)
            if (color == original) {
                scheme.setColor(colorKey as com.intellij.openapi.editor.colors.ColorKey, replacement)
                replacedCount++
            }
        }

        return replacedCount
    }

    /**
     * Update a text attribute in the scheme and reload the editor.
     *
     * @param scheme The editor color scheme to modify
     * @param attributeKeyName The name of the text attribute key
     * @param foreground The foreground color (or null)
     * @param background The background color (or null)
     * @param effectColor The effect color (or null)
     * @param effectType The effect type (or null)
     * @param fontType The font type (Font.BOLD | Font.ITALIC)
     */
    fun updateTextAttribute(
        scheme: EditorColorsScheme,
        attributeKeyName: String,
        foreground: Color?,
        background: Color?,
        effectColor: Color?,
        effectType: com.intellij.openapi.editor.markup.EffectType?,
        fontType: Int
    ) {
        val key = TextAttributesKey.find(attributeKeyName) ?: return

        val newAttrs = com.intellij.openapi.editor.markup.TextAttributes(
            foreground,
            background,
            effectColor,
            effectType,
            fontType
        )

        scheme.setAttributes(key, newAttrs)
        forceReloadScheme(scheme)
    }

    /**
     * Force the editor to reload the color scheme and update all open editors.
     * Should be called after making changes to a scheme.
     * @param scheme The editor color scheme to reload
     */
    fun forceReloadScheme(scheme: EditorColorsScheme) {
        val colorsManager = com.intellij.openapi.editor.colors.EditorColorsManager.getInstance()
        // If the scheme is bundled (read-only), clone and register as a new custom scheme
        val modifiableScheme = if (scheme.isReadOnly) {
            val clone = scheme.clone() as EditorColorsScheme
            clone.name = scheme.name + " (Paletteer)"
            colorsManager.addColorScheme(clone)
            clone
        } else {
            colorsManager.addColorScheme(scheme)
            scheme
        }
        colorsManager.setGlobalScheme(modifiableScheme)

        // Refresh all open editors to apply the new scheme
        val editorFactory = com.intellij.openapi.editor.EditorFactory.getInstance()
        editorFactory.refreshAllEditors()
    }
}
