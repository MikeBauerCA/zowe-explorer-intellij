/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.icons.AllIcons.Actions.Pause
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.impl.FileTypeRenderer
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import com.intellij.util.containers.toMutableSmartList
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.MFRemoteFileAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile
import javax.swing.SwingConstants

private val migratedIcon = AllIcons.FileTypes.Any_type


class FileLikeDatasetNode(
  file: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<*>,
  unit: ExplorerUnit,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<MFVirtualFile, ExplorerUnit>(
  file, project, parent, unit, treeStructure
), MFNode {

  override fun isAlwaysLeaf(): Boolean {
    return !value.isDirectory
  }

  override fun update(presentation: PresentationData) {
    val attributes = service<DataOpsManager>().tryToGetAttributes(value) as? RemoteDatasetAttributes
    val isMigrated = attributes?.isMigrated ?: false
    presentation.setIcon(if (value.isDirectory) AllIcons.Nodes.Folder else if (isMigrated) migratedIcon else AllIcons.FileTypes.Text)
    updateMainTitleUsingCutBuffer(value.presentableName, presentation)
    val volser = explorer.componentManager.service<DataOpsManager>()
      .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
      .getAttributes(value)?.volser
    volser?.let { presentation.addText(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.cachedChildren
      .map { FileLikeDatasetNode(value, notNullProject, this, unit, treeStructure) }.toMutableSmartList()
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }
}
