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
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.fetch.LibraryQuery
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile

class LibraryNode(
  library: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: FilesWorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<MFVirtualFile, LibraryQuery, FilesWorkingSet>(
  library, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {

  override val query: RemoteQuery<LibraryQuery, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig

      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(LibraryQuery(value), connectionConfig)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return map { FileLikeDatasetNode(it, notNullProject, this@LibraryNode, unit, treeStructure) }
  }

  override val requestClass = LibraryQuery::class.java

  override fun update(presentation: PresentationData) {
    presentation.setIcon(if (value.isDirectory) AllIcons.Nodes.Folder else AllIcons.FileTypes.Any_type)
    presentation.addText(value.presentableName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    val volser = explorer.componentManager.service<DataOpsManager>()
      .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
      .getAttributes(value)?.volser
    volser?.let { presentation.addText(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }

  override fun makeFetchTaskTitle(query: RemoteQuery<LibraryQuery, Unit>): String {
    return "Fetching members for ${query.request.library.name}"
  }
}
