package com.github.davidseptimus.paletteerintellijplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBSwingUtilities
import java.awt.BorderLayout

/**
 * Factory for creating the Paletteer tool window.
 */
class PaletteerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val paletteerToolWindow = PaletteerToolWindow(project)
        val content = ContentFactory.getInstance().createContent(paletteerToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}

/**
 * Main tool window that coordinates the lookup and replace panels.
 */
class PaletteerToolWindow(private val project: Project) {

    fun getContent() = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout()

        val splitter = JBSplitter(true, 0.5f).apply {
            // Lookup section (top)
            firstComponent = LookupPanel()

            // Replace section (bottom)
            secondComponent = ReplacePanel(project)
        }

        // Add padding around the splitter
        val paddedPanel = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            border = javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4) // 4px padding on all sides
            add(splitter, BorderLayout.CENTER)
        }

        add(paddedPanel, BorderLayout.CENTER)
    }
}
