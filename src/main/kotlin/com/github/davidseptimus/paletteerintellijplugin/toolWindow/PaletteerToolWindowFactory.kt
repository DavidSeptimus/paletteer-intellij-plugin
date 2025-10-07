package com.github.davidseptimus.paletteerintellijplugin.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout

/**
 * Factory for creating the Paletteer tool window.
 */
class PaletteerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val isVerticalDefault = toolWindow.anchor != ToolWindowAnchor.BOTTOM
        val paletteerToolWindow = PaletteerToolWindow(project, isVerticalDefault)
        val content = ContentFactory.getInstance().createContent(paletteerToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
        toolWindow.setTitleActions(listOf(paletteerToolWindow.toggleLayoutAction))

        var lastAnchor = toolWindow.anchor
        val connection = project.messageBus.connect()
        connection.subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                val currentAnchor = toolWindow.anchor
                if (currentAnchor != lastAnchor) {
                    val shouldBeVertical = currentAnchor != ToolWindowAnchor.BOTTOM
                    paletteerToolWindow.setLayout(shouldBeVertical)
                    lastAnchor = currentAnchor
                }
            }
        })
    }

    override fun shouldBeAvailable(project: Project) = true
}

/**
 * Main tool window that coordinates the lookup and replace panels.
 */
class PaletteerToolWindow(private val project: Project, initialVertical: Boolean) {
    private var isVertical = initialVertical
    private lateinit var splitter: JBSplitter

    fun setLayout(vertical: Boolean) {
        if (isVertical != vertical) {
            isVertical = vertical
            splitter.orientation = isVertical
            splitter.proportion = 0.9f
            splitter.firstComponent = LookupPanel(project)
            splitter.secondComponent = ReplacePanel(project)
            splitter.revalidate()
            splitter.repaint()
        }
    }

    val toggleLayoutAction = object : AnAction("Toggle Layout", null, AllIcons.Actions.SplitVertically) {

        override fun getActionUpdateThread() = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            isVertical = !isVertical
            splitter.orientation = isVertical
            splitter.proportion = 0.9f
            splitter.firstComponent = LookupPanel(project)
            splitter.secondComponent = ReplacePanel(project)
            splitter.revalidate()
            splitter.repaint()
        }
        override fun update(e: AnActionEvent) {
            e.presentation.icon = if (isVertical) AllIcons.Actions.SplitVertically else AllIcons.Actions.SplitHorizontally
        }
    }

    fun getContent() = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout()

        splitter = JBSplitter(isVertical, 0.9f).apply {
            firstComponent = LookupPanel(project)
            secondComponent = ReplacePanel(project)
        }

        val paddedPanel = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            border = javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4)
            add(splitter, BorderLayout.CENTER)
        }

        add(paddedPanel, BorderLayout.CENTER)
    }
}
