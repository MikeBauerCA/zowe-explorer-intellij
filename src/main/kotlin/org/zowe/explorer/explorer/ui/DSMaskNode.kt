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
import com.intellij.util.containers.toMutableSmartList
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.vfs.MFVirtualFile

class DSMaskNode(
  dsMask: DSMask,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: FilesWorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<DSMask, DSMask, FilesWorkingSet>(
  dsMask, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {

  override fun update(presentation: PresentationData) {
    presentation.addText(value.mask, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.addText(" ${value.volser}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    presentation.setIcon(AllIcons.Nodes.Module)
  }

  override val query: RemoteQuery<DSMask, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(value, connectionConfig)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): MutableList<AbstractTreeNode<*>> {
    return map {
      if (it.isDirectory) {
        LibraryNode(it, notNullProject, this@DSMaskNode, unit, treeStructure)
      } else {
        FileLikeDatasetNode(it, notNullProject, this@DSMaskNode, unit, treeStructure)
      }
    }.toMutableSmartList()
  }

  override val requestClass = DSMask::class.java

  override fun makeFetchTaskTitle(query: RemoteQuery<DSMask, Unit>): String {
    return "Fetching listings for ${query.request.mask}"
  }


}
