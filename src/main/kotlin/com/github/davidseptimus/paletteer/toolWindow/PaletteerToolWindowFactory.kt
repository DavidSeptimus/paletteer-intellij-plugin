package com.github.davidseptimus.paletteer.toolWindow

import com.github.davidseptimus.paletteer.PaletteerBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import java.awt.BorderLayout

/**
 * Factory for creating the Paletteer tool window.
 */
class PaletteerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        // Create Color Theme tab
        val isVerticalDefault = toolWindow.anchor != ToolWindowAnchor.BOTTOM
        val paletteerToolWindow = PaletteerToolWindow(project, isVerticalDefault, toolWindow)
        val colorThemeContent = contentFactory.createContent(
            paletteerToolWindow.getContent(),
            PaletteerBundle.message("toolWindow.tab.colorTheme"),
            false
        )

        // Register panels as disposables with the content
        Disposer.register(colorThemeContent, paletteerToolWindow.lookupPanel)
        Disposer.register(colorThemeContent, paletteerToolWindow.replacePanel)

        toolWindow.contentManager.addContent(colorThemeContent)
        toolWindow.setTitleActions(listOf(paletteerToolWindow.toggleLayoutAction))

        var lastAnchor = toolWindow.anchor
        val connection = project.messageBus.connect(colorThemeContent)
        connection.subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                val currentAnchor = toolWindow.anchor
                if (currentAnchor != lastAnchor) {
                    val shouldBeVertical = currentAnchor != ToolWindowAnchor.BOTTOM
                    paletteerToolWindow.setLayout(shouldBeVertical)
                    lastAnchor = currentAnchor
                }

                // Restore state when tool window becomes visible
                if (toolWindow.isVisible && toolWindow.contentManager.selectedContent == colorThemeContent) {
                    paletteerToolWindow.lookupPanel.restoreState()
                }
            }
        })

        // Create Export tab
        val exportPanel = ExportPanel(project)
        val exportContent = contentFactory.createContent(
            exportPanel,
            PaletteerBundle.message("toolWindow.tab.export"),
            false
        )
        Disposer.register(exportContent, exportPanel)
        toolWindow.contentManager.addContent(exportContent)

        // Listen for tab selection changes
        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                // Restore state when Color Theme tab is selected
                if (event.content == colorThemeContent && event.content.isSelected) {
                    paletteerToolWindow.lookupPanel.restoreState()
                }
            }
        })
    }

    override fun shouldBeAvailable(project: Project) = true
}

/**
 * Main tool window that coordinates the lookup and replace panels.
 */
class PaletteerToolWindow(private val project: Project, initialVertical: Boolean, private val toolWindow: ToolWindow) {
    private var isVertical = initialVertical
    private lateinit var splitter: JBSplitter

    var lookupPanel: LookupPanel = LookupPanel(project)
        private set
    var replacePanel: ReplacePanel = ReplacePanel(project)
        private set

    fun setLayout(vertical: Boolean) {
        if (isVertical != vertical) {
            isVertical = vertical
            splitter.orientation = isVertical
            splitter.proportion = 0.9f

            // Dispose old panels
            Disposer.dispose(lookupPanel)
            Disposer.dispose(replacePanel)

            // Create new panels
            lookupPanel = LookupPanel(project)
            replacePanel = ReplacePanel(project)

            splitter.firstComponent = lookupPanel
            splitter.secondComponent = replacePanel
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

            // Dispose old panels
            Disposer.dispose(lookupPanel)
            Disposer.dispose(replacePanel)

            // Create new panels
            lookupPanel = LookupPanel(project)
            replacePanel = ReplacePanel(project)

            splitter.firstComponent = lookupPanel
            splitter.secondComponent = replacePanel
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
            firstComponent = lookupPanel
            secondComponent = replacePanel
        }

        val paddedPanel = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            border = javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4)
            add(splitter, BorderLayout.CENTER)
        }

        add(paddedPanel, BorderLayout.CENTER)
    }
}
