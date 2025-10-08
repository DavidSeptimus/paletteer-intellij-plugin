package com.github.davidseptimus.paletteer.toolWindow

import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

/**
 * Document filter that only allows hexadecimal color input (0-9, A-F).
 * Automatically strips out # and other invalid characters.
 */
class HexColorDocumentFilter : DocumentFilter() {
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

