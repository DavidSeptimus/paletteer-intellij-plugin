package com.github.davidseptimus.paletteer.util

import java.awt.Color

fun Color.toHex(): String {
    return if (this.alpha == 255) "%06X".format(this.rgb and 0xFFFFFF)
    else "%08X".format(this.rgb)
}

fun Color.toCssHex(): String {
    return "#${this.toHex()}"
}