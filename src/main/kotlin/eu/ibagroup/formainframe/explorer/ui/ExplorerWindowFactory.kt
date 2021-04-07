package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import eu.ibagroup.formainframe.utils.appService

class ExplorerWindowFactory : ToolWindowFactory, DumbAware {

  override fun isApplicable(project: Project): Boolean {
    return true
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow
    val contentFactory = ContentFactory.SERVICE.getInstance()
    val factory = appService<ExplorerContent>()
    val content = contentFactory
      .createContent(factory.buildComponent(toolWindow.disposable, project), factory.displayName, factory.isLockable)
    toolWindow.contentManager.addContent(content)
  }

  override fun init(toolWindow: ToolWindow) {}

  override fun shouldBeAvailable(project: Project): Boolean {
    return true
  }
}